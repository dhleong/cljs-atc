(ns atc.util.interceptors
  (:require
   [re-frame.core :refer [->interceptor get-coeffect get-effect]]
   [re-frame.interceptor :refer [assoc-effect]]))

(defn persist-key
  ([data-key] (persist-key data-key :as data-key))
  ([db-key _ data-key]
   (->interceptor
     :id (keyword "persist-key" (name data-key))
     :after (fn [context]
              (let [initial-prefs (db-key (get-coeffect context :db {}) ::not-found)
                    resulting-prefs (db-key (get-effect context :db {}) ::not-found)]
                (if-not (or (= ::not-found initial-prefs)
                            (= ::not-found resulting-prefs)
                            (= initial-prefs resulting-prefs))
                  (assoc-effect
                    context
                    :local-storage/save
                    [data-key resulting-prefs])
                  context))))))
