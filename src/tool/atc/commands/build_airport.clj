(ns atc.commands.build-airport
  (:require
   [atc.config :as config]
   [atc.engine.model :refer [vec3]]
   [atc.game.traffic.shared-util :refer [partial-arrival-route]]
   [atc.subs-util :refer [navaids-by-id]]
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
                                     (into {}))]
    (merge airport
             {:arrival-navaid-grouping arrival-navaid-grouping})))
