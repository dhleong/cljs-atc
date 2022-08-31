(ns atc.data.airports 
  (:require
   [atc.data.core :refer [local-xy]]))

(defn runway-coords [airport runway]
  (when-let [runway-object (->> airport
                                :runways
                                (filter #(or (= runway (:start-id %))
                                             (= runway (:end-id %))))
                                first)]
    (let [start (local-xy (:start-threshold runway-object) airport)
          end (local-xy (:end-threshold runway-object) airport)]
      (if (= (:start-id runway-object) runway)
        [start end]
        [end start]))))
