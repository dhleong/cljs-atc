(ns atc.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx
                                   path
                                   trim-v]]
            [atc.db :as db]))

(reg-event-db
  ::initialize-db
  (fn [_ _]
    db/default-db))

(reg-event-db
  :navigate!
  [trim-v]
  (fn [db page-spec]
    (assoc db :page page-spec)))

(reg-event-fx
  :voice/set-paused
  [trim-v (path :voice)]
  (fn [{voice :db} [paused?]]
    {:voice/set-paused paused?
     :db (assoc voice :paused? paused?)}))

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
    (println "TODO: voice result: " result)))

(reg-event-fx
  :voice/set-state
  [(path :voice) trim-v]
  (fn [{voice :db} [new-state]]
    ; Default to "paused" if we didn't request to start immediately
    (let [will-be-paused? (:paused? voice true)]
      (println "state <- " new-state "; paused = " will-be-paused?)
      {:db (assoc voice :state new-state
                  :paused? will-be-paused?)
       :fx [(when (and (= :ready new-state)
                       will-be-paused?)
              [:voice/set-paused true])]})))
