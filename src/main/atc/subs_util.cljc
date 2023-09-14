(ns atc.subs-util
  (:require
   [atc.data.core :refer [local-xy]]))

(defn airport-runway-ids [airport]
  (when airport
    (->> airport
         :runways
         (mapcat (juxt :start-id :end-id))
         (into #{}))))

(defn navaids-by-id [airport]
  (some->>
    airport
    :navaids
    (reduce
      (fn [m {:keys [position] :as navaid}]
        (assoc m (:id navaid)
               (merge navaid (local-xy position airport))))
      {})))
