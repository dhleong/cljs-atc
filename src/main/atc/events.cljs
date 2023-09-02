(ns atc.events
  (:require
   [atc.cofx :as cofx]
   [atc.db :as db]
   [atc.engine.core :as engine]
   [atc.engine.model :as engine-model]
   [atc.game.keymap :as keymap]
   [atc.radio :refer [->readable ->speakable]]
   [atc.util.interceptors :refer [persist-key]]
   [atc.util.local-storage :as local-storage]
   [atc.util.spec :refer [pre-validate]]
   [atc.weather.fx :as weather-fx]
   [atc.weather.spec :refer [default-wx weather-spec]]
   [clojure.math :refer [floor]]
   [clojure.string :as str]
   [re-frame.core :refer [->interceptor dispatch get-coeffect get-effect
                          inject-cofx path reg-event-db reg-event-fx trim-v unwrap]]
   [re-frame.interceptor :refer [update-coeffect update-effect]]
   [re-pressed.core :as rp]
   [vimsical.re-frame.cofx.inject :as inject]))

(def seconds-between-game-snapshots 4)
(def ui-tick-delay-ms 2000)

; ======= Engine data injection ===========================

(def injected-subscriptions
  [[:game/active-runways]
   [:game/airport-runway-ids]
   [:game/navaids-by-id]])

(def engine-injections
  (->> injected-subscriptions
       (map #(inject-cofx ::inject/sub %))))

(def injected-subscription-keys (map first injected-subscriptions))

(defn merge-injections [engine cofx]
  (merge engine (select-keys cofx injected-subscription-keys)))

(defn remove-injections [engine]
  (apply dissoc engine injected-subscription-keys))

(defn- compose-cofx [context cofx-key cofx]
  (reduce
    (fn [ctx cofx-item]
      (if-some [handler (get cofx-item cofx-key)]
        (handler ctx)
        ctx))
    context
    cofx))

; This interceptor composes the subscription cofx listed above, assoc's them into the
; :engine in the DB for dispatching in updates, etc. then cleans them up after. It should
; be provided *before* any (path) interceptors
(def injected-engine
  (->interceptor
    {:id :injected-engine
     :before (fn [context]
               (let [context' (compose-cofx context :before engine-injections)]
                 (if (not= ::not-found (get (get-coeffect context' :db) :engine ::not-found))
                   (update-coeffect context' :db
                                    update :engine
                                    merge-injections (get-coeffect context'))
                   context')))
     :after (fn [context]
              (let [context' (compose-cofx context :after engine-injections)]
                (if (not= ::not-found (get-effect context' :db ::not-found))
                  (update-effect context' :db update :engine remove-injections)
                  context')))}))

; ======= Events ==========================================

(reg-event-fx
  ::initialize-db
  [(inject-cofx ::local-storage/load :game-options)]
  (fn [{:keys [game-options]} _]
    {:db (assoc db/default-db :game-options game-options)}))

; This event is unused... for now (and that's okay)
#_:clj-kondo/ignore
(reg-event-db
  :navigate!
  [trim-v]
  (fn [db page-spec]
    (assoc db :page page-spec)))

(reg-event-db
  :game/command
  [injected-engine trim-v (path :engine)]
  (fn [engine [command]]
    (when engine
      (println "dispatching command: " command)
      (engine-model/command engine command))))

(reg-event-fx
  :game/init
  [unwrap (persist-key :game-options)]
  (fn [{:keys [db]} game-options]
    {:db (-> db
             (dissoc :last-game)  ; We can release this memory, now
             (assoc :game-options game-options))
     :fx [[:dispatch [:weather/refresh]]]
     :game/init-async game-options}))

(reg-event-fx
  :game/resume-last
  [unwrap]
  (fn [{:keys [db]}]
    (if-some [last-game (:last-game db)]
      {:db (-> db
               (dissoc :last-game)
               (merge last-game))
       :fx [[:dispatch
             [:game/init-loaded-engine
              (assoc (:engine-config last-game)
                     :engine (-> (:engine last-game)
                                 (assoc :time-scale 1)))]]]}

      (throw (ex-info "Invalid state: cannot :game/resume-last without last-game"
                      {})))))

(reg-event-fx
  :game/init-loaded-engine
  [unwrap]
  (fn [{:keys [db]} {:keys [engine arrivals? departures? voice-input?]}]
    {:db (assoc db
                :ui/tick 0
                :engine engine
                :engine-config {:voice-input? voice-input?
                                :arrivals? arrivals?
                                :departures? departures?})
     :dispatch-n [[:game/tick]
                  [:ui/tick]
                  (when voice-input?
                    ; NOTE: We don't start voice until the engine is loaded,
                    ; since the voice grammar depends on the airport
                    [:voice/start!])]}))

(reg-event-fx
  :game/tick
  [injected-engine]
  (fn [{:keys [db]} _]
    (when-let [engine (:engine db)]
      (let [engine' (engine-model/tick engine nil)
            event-metadata {:elapsed-s (:elapsed-s engine')}
            new-events (map
                         (fn [event-vec]
                           (conj event-vec event-metadata))
                         (:events engine'))
            engine' (assoc engine' :events [])

            db' (-> db
                    (assoc :engine engine')
                    (update :game-events into new-events))

            seconds-since-last-snapshot (- (:elapsed-s engine')
                                           (:elapsed-s (peek (:game-history db))))]
        {:db (if (>= seconds-since-last-snapshot seconds-between-game-snapshots)
               (update db' :game-history conj engine')
               db')
         :fx [(when-let [delay-ms (engine/next-tick-delay engine')]
                [:dispatch-later
                 {:ms delay-ms
                  :dispatch [:game/tick]}])

              (when (seq new-events)
                (println "TODO: Handle engine events:" new-events))]}))))

(reg-event-fx
  :ui/tick
  (fn [{:keys [db]} _]
    {:db (update db :ui/tick inc)
     :fx [(when (some? (engine/next-tick-delay (:engine db)))
            ; Only dispatch when not paused
            [:dispatch-later
             {:ms ui-tick-delay-ms
              :dispatch [:ui/tick]}])]}))

(reg-event-fx
  :game/reset
  [trim-v]
  (fn [{:keys [db]} _]
    (println "Clear game engine")
    {:db (-> db
             (dissoc :engine :engine-config)
             (update :game-events empty)
             (update :game-history empty)
             (update :radio-history empty)
             (assoc :last-game (select-keys db [:engine :engine-config
                                                :game-events :game-history
                                                :radio-history])))
     :dispatch [:voice/stop!]}))

(reg-event-fx
  :game/set-time-scale
  [trim-v]
  (fn [{{:keys [engine engine-config] :as db} :db} [scale]]
    (let [voice-enabled? (:voice-input? engine-config)]
      {:db (assoc db :engine
                  (cond-> (assoc engine :time-scale scale)
                    (= 0 scale) (assoc :last-tick nil)))
       :fx [(when-not (= 0 scale)
              [:dispatch [:game/tick]])

            (when-not (= 0 scale)
              [:dispatch [:ui/tick]])

            (when (not= scale (:time-scale engine))
              (cond
                ; Pausing; disable voice
                (= 0 scale)
                [:dispatch [:voice/stop!]]

                ; Resuming, and voice was requested
                voice-enabled?
                [:dispatch [:voice/start!]]))]})))

(reg-event-fx
  :game/toggle-paused
  [(path :engine)]
  (fn [{engine :db}]
    {:dispatch [:game/set-time-scale
                (if (= 0 (:time-scale engine))
                  1
                  0)]}))


; ======= Speech synthesis ================================

(reg-event-fx
  :speech/enqueue
  [trim-v (path :speech)]
  (fn [{speech :db} [{:keys [from message] :as obj}]]
    ; TODO: Save all "readable" radio comms for (optional) rendering
    (let [speakable (->speakable message)]
      (println "enqueue: " obj " -> " speakable)
      {:db (update speech :queue conj {:message speakable
                                       ::raw obj
                                       :voice (:voice from)})
       :dispatch [::speech-check-queue]})))

(reg-event-fx
  ::speech-check-queue
  [trim-v]
  (fn [{{:keys [speech] :as db} :db}]
    (when (and (not (:speaking? speech))
               (or (:paused? (:voice db))
                   (nil? (:voice db))))
      (when-let [enqueued (first (:queue speech))]
        {:db (-> db
                 (assoc-in [:speech :speaking?] true)
                 (update-in [:speech :queue] pop))
         :speech/say (assoc enqueued :on-complete #(dispatch [::speech-utterance-complete]))
         :dispatch (let [obj (::raw enqueued)]
                     [:radio-history/push
                      {:speaker (:name (:from obj))
                       :freq 127.1 ; TODO get this from :engine
                       :text (:message obj)}])}))))

(def delay-between-enqueued-radio-ms 1250)

(reg-event-fx
  ::speech-utterance-complete
  [trim-v (path :speech)]
  (fn [{speech :db}]
    (println "[speech] utterance complete")
    {:db (assoc speech :speaking? false)
     :dispatch-later {:dispatch [::speech-check-queue]
                      ; TODO Perhaps, randomize this a bit:
                      :ms delay-between-enqueued-radio-ms}}))

(reg-event-db
  :speech/unavailable
  [(path :speech)]
  (fn [speech _]
    (assoc speech :available? false)))

(reg-event-db
  :speech/on-voices-changed
  [trim-v (path :speech)]
  (fn [speech [voices]]
    (assoc speech :voices voices :available? true)))


; ======= Voice input =====================================

(reg-event-fx
  :voice/set-paused
  [trim-v (path :voice)]
  (fn [{voice :db} [paused?]]
    {:voice/set-paused paused?
     :db (assoc voice :paused? paused?)
     :dispatch-n [(when paused?
                    [::voice-process-pending])

                  ; TODO: probably, do this after some delay... although,
                  ; realistically, any responses to this input will probably
                  ; need to be *inserted* into the queue...
                  [::speech-check-queue]]}))

(reg-event-db
  :voice/busy
  [trim-v (path :voice)]
  (fn [voice [busy?]]
    (assoc voice :busy? busy?)))

(reg-event-fx
  :voice/enable-keypresses
  (fn []
    {:dispatch-n [[::rp/set-keydown-rules
                   keymap/keydown-rules]
                  [::rp/set-keypress-rules
                   keymap/keypress-rules]
                  [::rp/set-keyup-rules
                   keymap/keyup-rules]]}))

(reg-event-fx
  :voice/disable-keypresses
  (fn []
    {:dispatch-n [[::rp/set-keydown-rules
                   {:event-keys []}]
                  [::rp/set-keypress-rules
                   {:event-keys []}]
                  [::rp/set-keyup-rules
                   {:event-keys []}]]}))


(reg-event-fx
  :voice/start!
  [trim-v]
  (fn [{:keys [db]} [?opts]]
    (when-not (:engine db)
      (println "WARNING: Initializing voice without a game engine"))
    {:db (assoc db :voice {:pending-results []})
     :voice/start! (assoc ?opts
                          :engine (:engine db))}))

(reg-event-fx
  :voice/stop!
  (fn [{:keys [db]}]
    {:voice/stop! true
     :db (dissoc db :voice)}))

(reg-event-db
  :voice/on-partial
  [trim-v]
  (fn [db [partial-text]]
    (assoc-in db [:voice :partial-text] partial-text)))

(reg-event-fx
  :voice/on-result
  [trim-v]
  (fn [{:keys [db]} [result]]
    (println "voice/on-result" result (:voice db))
    {:db (update-in db [:voice :pending-results] conj result)
     :dispatch [::voice-process-pending]}))

(reg-event-fx
  ::voice-process-pending
  [trim-v]
  (fn [{:keys [db]}]
    (println "voice/process? :" (:voice db))
    (when (and
            ; If not :paused? the user is still holding down PTT---IE they're not
            ; done with this input!
            (:paused? (:voice db))

            ; IE: Not still waiting for a result:
            (empty? (get-in db [:voice :partial-text]))
            (false? (get-in db [:voice :busy?]))

            (seq (get-in db [:voice :pending-results])))
      (let [full-input (str/join " " (get-in db [:voice :pending-results]))]
        {:dispatch [::voice-handle-text full-input]}))))

(reg-event-fx
  ::voice-handle-text
  [trim-v (inject-cofx ::cofx/now)]
  (fn [{:keys [db now]} [full-input]]
    (println "voice/process:" full-input (:voice db))
    {:db (-> db
             (update-in [:voice :pending-results] empty)
             (update :radio-history conj {:speaker "CTR" ; TODO get this from :engine
                                          :freq 127.1 ; TODO this, too
                                          :timestamp now
                                          :text full-input
                                          :self? true}))
     :voice/process {:machine (:parsing-machine (:engine db))
                     :input full-input}}))

(reg-event-fx
  :voice/set-state
  [trim-v (path :voice)]
  (fn [{voice :db} [new-state]]
    ; Default to "paused" if we didn't request to start immediately
    (let [will-be-paused? (:paused? voice true)]
      (println "state <- " new-state "; paused = " will-be-paused?)
      {:db (assoc voice :state new-state
                  :paused? will-be-paused?)
       :fx [(when (and (= :ready new-state)
                       will-be-paused?)
              [:dispatch [:voice/set-paused true]])]})))


; ======= UI state ========================================

(reg-event-fx
  :radio-history/push
  [unwrap (path :radio-history) (inject-cofx ::cofx/now)]
  (fn [{radio-history :db :keys [now]} message]
    {:db (conj radio-history
               (-> message
                   (assoc :timestamp now)
                   (update :text ->readable)))}))

; ======= UI Config =======================================

; TODO auto-persist these + wire up
(reg-event-db
  :config/set
  [unwrap (path :ui-config)]
  (fn [config updates]
    (merge config updates)))


; ======= Weather =========================================

(reg-event-fx
  :weather/refresh
  (fn [{:keys [db]}]
    {::weather-fx/fetch (name (get-in db [:game-options :airport-id]))}))

(reg-event-fx
  :weather/failed
  [trim-v]
  (fn [_ [airport-icao]]
    {:dispatch [:weather/fetched airport-icao default-wx]}))

(defn- increment-atis [code now]
  (case code
    nil (String/fromCharCode
          (+ (.charCodeAt "A" 0)
             (mod (floor (/ now 60000)) 25)))
    "Z" "A"
    (String/fromCharCode (inc (.charCodeAt "B" 0)))))

(defn- update-weather [db now wx]
  (let [wx-keys [:wind-heading :altimeter]
        wx-changed? (not= (select-keys wx wx-keys)
                          (select-keys
                            (get-in db [:engine :weather])
                            wx-keys))]
    (cond-> db
      wx-changed? (update-in [:engine :weather :atis] increment-atis now)
      true (assoc-in [:engine :weather] wx))))

(reg-event-fx
  :weather/fetched
  [trim-v (inject-cofx ::cofx/now)]
  (fn [{now :now db :db} [airport-icao wx]]
    {:pre [(pre-validate weather-spec wx)]}
    (let [current-airport (get-in db [:game-options :airport-id])]
      (cond-> db
        (= airport-icao (name current-airport))
        (update-weather now wx)))))


; ======= "Help" functionality ============================

(reg-event-fx
  :help/identify-navaid
  [trim-v injected-engine (path :engine)]
  (fn [{engine :db} [id]]
    (let [navaid (get-in engine [:game/navaids-by-id id])]
      {:dispatch [:radio-history/push
                  {:speaker :system/help
                   :text [id (name (:type navaid))
                          ": pronounced " (->speakable [navaid])]}]})))

(reg-event-fx
  :help/identify-untracked
  [trim-v]
  (fn [_ [track]]
    (let [msg (cond
                (nil? track)
                "An untracked aircraft, on the ground or flying VFR."

                (= "C" (:track-symbol track))
                "An aircraft tracked by another Center controller."

                (= "T" (:track-symbol track))
                "An aircraft tracked by a Tower controller."

                :else
                "Huh. Not sure what that is myself (this is a bug)")]
      {:dispatch [:radio-history/push
                  {:speaker :system/help
                   :text msg}]})))

(reg-event-fx
  :help/warning
  [trim-v]
  (fn [_ message]
    {:dispatch [:radio-history/push
                {:speaker :system/warning
                 :text message}]}))

(comment
  (dispatch [:game/reset])

  (dispatch [:config/set {:range-rings-nm 10}])

  (dispatch [:weather/refresh])

  (dispatch [::voice-handle-text "delta twenty two turn right heading one eight zero"])
  (dispatch [::voice-handle-text "delta twenty two contact center point eight good day"]))
