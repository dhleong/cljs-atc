(ns atc.data.airports 
  (:require
   [atc.data.core :refer [local-xy]]
   [atc.data.units :refer [ft->m]]
   [atc.engine.model :refer [vec3]]))

(defn runway-coords [airport runway]
  (when-let [runway-object (->> airport
                                :runways
                                (filter #(or (= runway (:start-id %))
                                             (= runway (:end-id %))))
                                first)]
    (let [elevation (-> (:position airport)
                        (nth 2)
                        ft->m)
          start (-> (local-xy (:start-threshold runway-object) airport)
                    (vec3 elevation))
          end (-> (local-xy (:end-threshold runway-object) airport)
                  (vec3 elevation))]
      (if (= (:start-id runway-object) runway)
        [start end]
        [end start]))))
