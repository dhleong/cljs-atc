(ns atc.events
  (:require
   [atc.data.airports.kjfk :as kjfk]
   [atc.data.core :refer [local-xy]]
   [atc.db :as db]
   [atc.engine.core :as engine]
   [atc.engine.model :as engine-model]
   [goog.events.KeyCodes :as KeyCodes]
   [re-frame.core :refer [->interceptor dispatch get-coeffect get-effect
                          inject-cofx path reg-event-db reg-event-fx trim-v]]
   [re-frame.interceptor :refer [update-coeffect update-effect]]
   [re-pressed.core :as rp]
   [vimsical.re-frame.cofx.inject :as inject]))

(def injected-subscriptions
  [[:game/navaids-by-id]])

(def engine-injections
  (->> injected-subscriptions
       (map #(inject-cofx ::inject/sub %))))

(def injected-subscription-keys (map first injected-subscriptions))

(defn merge-injections [engine cofx]
  (merge engine (select-keys cofx injected-subscription-keys)))

(defn remove-injections [engine]
  (apply dissoc engine injected-subscription-keys))

; This interceptor composes the subscription cofx listed above, assoc's them into the
; :engine in the DB for dispatching in updates, etc. then cleans them up after. It should
; be provided *before* any (path) interceptors
(def injected-engine
  (->interceptor
    {:id :injected-engine
     :before (fn [context]
               (let [ctx' (reduce
                            (fn [ctx {:keys [before]}]
                              (if before
                                (before ctx)
                                ctx))
                            context
                            engine-injections)]
                 (update-coeffect ctx' :db
                                  update :engine
                                  merge-injections (get-coeffect ctx'))))
     :after (fn [context]
              (let [context' (reduce
                               (fn [ctx {:keys [after]}]
                                 (if after
                                   (after ctx)
                                   ctx))
                               context
                               engine-injections)]
                (if (not= ::not-found (get-effect context' :db ::not-found))
                  (update-effect context' :db update :engine remove-injections)
                  context')))}))

; ======= Subscriptions ===================================

(reg-event-db
  ::initialize-db
  (fn [_ _]
    db/default-db))

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
  [trim-v]
  (fn [{:keys [db]} [airport]]
    (let [airport (or airport kjfk/airport)]
      (println "(re) Initialize game engine")
      (let [new-engine (engine/generate airport)]
        (println "AC" (-> new-engine :aircraft vals first :position))
        (let [lga (-> new-engine :airport :navaids (nth 2))]
          (println (:id lga) (local-xy (-> lga :position)
                                       (-> new-engine :airport))))
        {:db (assoc db :engine new-engine)
         :dispatch [:game/tick]}))))

(reg-event-fx
  :game/tick
  [injected-engine (path :engine)]
  (fn [{engine :db} _]
    (when engine
      (let [updated-engine (engine-model/tick engine nil)]
        {:db updated-engine
         :fx [(when-let [delay-ms (engine/next-tick-delay updated-engine)]
                [:dispatch-later
                 {:ms delay-ms
                  :dispatch [:game/tick]}])]}))))

(reg-event-db
  :game/reset
  [trim-v]
  (fn [db _]
    (println "Clear game engine")
    (dissoc db :engine)))

(reg-event-fx
  :game/set-time-scale
  [trim-v (path :engine)]
  (fn [{engine :db} [scale]]
    {:db (cond-> (assoc engine :time-scale scale)
           (= 0 scale) (assoc :last-tick nil))
     :fx [(when-not (= 0 scale)
            [:dispatch [:game/tick]])]}))


; ======= Speech synthesis ================================

(reg-event-fx
  :speech/enqueue
  [trim-v (path :speech)]
  (fn [{speech :db} [{:keys [from message] :as obj}]]
    (println "enqueue: " obj)
    {:db (update speech :queue conj {:message message
                                     :voice (:voice from)})
     :dispatch [::speech-check-queue]}))

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
         :speech/say (assoc enqueued :on-complete #(dispatch [::speech-utterance-complete]))}))))

(def delay-between-enqueued-radio-ms 1250)

(reg-event-fx
  ::speech-utterance-complete
  [trim-v (path :speech)]
  (fn [{speech :db}]
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
     :dispatch [::speech-check-queue]}))

(reg-event-db
  :voice/busy
  [trim-v (path :voice)]
  (fn [voice [busy?]]
    (assoc voice :busy? busy?)))

(reg-event-fx
  :voice/enable-keypresses
  (fn []
    {:dispatch-n [[::rp/set-keydown-rules
                   {:event-keys [[[:voice/set-paused false]
                                  [{:keyCode KeyCodes/SPACE}]]]}]
                  [::rp/set-keyup-rules
                   {:event-keys [[[:voice/set-paused true]
                                  [{:keyCode KeyCodes/SPACE}]]]}]]}))

(reg-event-fx
  :voice/disable-keypresses
  (fn []
    {:dispatch-n [[::rp/set-keydown-rules
                   {:event-keys []}]
                  [::rp/set-keyup-rules
                   {:event-keys []}]]}))


(reg-event-fx
  :voice/start!
  [trim-v]
  (fn [{:keys [db]} [?opts]]
    (when-not (:engine db)
      (println "WARNING: Initializing voice without a game engine"))
    {:voice/start! (assoc ?opts
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
    (println "voice/on-result" result)
    {:voice/process {:machine (:parsing-machine (:engine db))
                     :input result}}))

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
              [:voice/set-paused true])]})))

(comment
  (dispatch [:game/reset]))
