(ns atc.events
  (:require
   [atc.db :as db]
   [atc.engine.core :as engine]
   [atc.engine.model :as engine-model]
   [re-frame.core :refer [dispatch path reg-event-db reg-event-fx trim-v]]))

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
  [trim-v (path :engine)]
  (fn [engine [command]]
    (println "dispatching command: " command)
    (engine-model/command engine command)))

(reg-event-fx
  :game/init
  [trim-v]
  (fn [{:keys [db]} _]
    (println "(re) Initialize game engine")
    (let [new-engine (engine/generate)]
      {:db (assoc db :engine new-engine)
       :dispatch [:game/tick]})))

(reg-event-fx
  :game/tick
  [(path :engine)]
  (fn [{engine :db} _]
    (when engine
      (let [updated-engine (engine-model/tick engine nil)]
        ; TODO: Maybe here, dispatch the next queued radio call (if not transmitting)
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

(reg-event-fx
  :voice/start!
  [trim-v]
  (fn [_ [?opts]]
    {:voice/start! ?opts}))

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
  (fn [_ [result]]
    {:voice/process result}))

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
