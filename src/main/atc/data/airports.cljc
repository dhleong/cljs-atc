(ns atc.data.airports
  (:require
   [atc.data.core :refer [local-xy]]
   [atc.data.units :refer [ft->m]]
   [atc.engine.model :refer [vec3]]
   [shadow.lazy :as lazy]))

; NOTE: We explicitly do NOT want to require these namespaces,
; since they should be code-split
#_{:clj-kondo/ignore [:unresolved-namespace]}
(def ^:private airport-loadables
  {:kjfk (lazy/loadable atc.data.airports.kjfk/airport)})

(defn load-airport [airport-id]
  (if-some [loadable (get airport-loadables airport-id)]
    (if (lazy/ready? loadable)
      @loadable
      (lazy/load loadable))
    (throw (ex-info "No such airport: " {:id airport-id}))))

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
