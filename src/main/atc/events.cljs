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

(reg-event-db
  :voice/set-state
  [(path :voice) trim-v]
  (fn [voice [new-state]]
    (println "state <- " new-state)
    (assoc voice :state new-state)))
