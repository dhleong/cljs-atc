(ns atc.commands.build-airport
  (:require
   [atc.config :as config]
   [atc.engine.model :refer [vec3]]
   [atc.game.traffic.shared-util :refer [partial-arrival-route]]
   [atc.subs-util :refer [navaids-by-id]]
   [atc.util.numbers :refer [->int]]
   [atc.util.testing :refer [roughly=]]))

(defn- overlapping-navaids [navaids]
  (->> (for [n1 navaids
             n2 navaids]
         (when-not (= n1 n2)
           (when (roughly= (vec3 n1 0) (vec3 n2 0)
                           :delta (/ config/lateral-spacing-m 2))
             #{(:id n1) (:id n2)})))
       (keep identity)
       (distinct)
       (map vec)))

(defn- runway->heading [runway]
  (-> (->int runway)
      (* 10)))

(defn- compile-runway-selection [airport]
  ; This is very rough and definitely not real-world accurate, but is maybe
  ; sufficient for now
  (let [grouped-runways (concat
                          (->> airport
                               :runways
                               (group-by (comp runway->heading :start-id))
                               (map (partial into [:start-id])))
                          (->> airport
                               :runways
                               (group-by (comp runway->heading :end-id))
                               (map (partial into [:end-id]))))
        angle-range (/ 360 (count (keys grouped-runways)))
        angle-radius (/ angle-range 2)
        conditions (->> grouped-runways
                        (map
                          (fn [[runway-key angle runways]]
                            [angle (mapv runway-key runways)]))
                        (mapcat (fn [[angle runway-ids]]
                                  [`(<= ~(- angle angle-radius)
                                        ~'wind-heading
                                        ~(+ angle angle-range))
                                   {:arrivals (vec (take 1 runway-ids))
                                    :departures (vec (take-last 1 runway-ids))}]))
                        (cons `cond))]
    `(fn [~'weather]
       (let [~'wind-heading (:wind-heading ~'weather)]
         ~conditions))))

(defn augment-airport [airport]
  (let [engine {:airport airport
                :game/navaids-by-id (navaids-by-id airport)}
        arrival-navaids (->> airport
                             :arrival-routes
                             vals
                             (map (comp
                                    last
                                    (partial partial-arrival-route engine)))
                             (distinct)
                             (map #(get-in engine [:game/navaids-by-id %])))

        ; A map of fix -> fix for all pairs of navaids that are
        ; unacceptably close together.
        arrival-navaid-grouping (->> arrival-navaids
                                     (overlapping-navaids)
                                     (into {}))

        runway-selection (compile-runway-selection airport)]
    (merge airport
             {:runway-selection runway-selection
              :arrival-navaid-grouping arrival-navaid-grouping})))
