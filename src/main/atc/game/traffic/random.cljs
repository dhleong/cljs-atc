(ns atc.game.traffic.random
  (:require
   [atc.data.aircraft-configs :as configs]
   [atc.data.airlines :refer [all-airlines]]
   [atc.game.traffic.model :refer [ITraffic next-arrival]]
   [atc.game.traffic.shared :refer [partial-arrival-route
                                    space-crafts-along-route]]
   [atc.util.seedable :refer [next-int pick-random]]))

(defrecord RandomTraffic [random]
  ITraffic
  (generate-initial-arrivals [this {:keys [airport] :as engine}]
    (let [crafts (repeatedly
                   10 ; TODO "difficulty"?
                   #(next-arrival this engine))]
      ; Position at intervals along arrival route
      (->> crafts
           (group-by (partial partial-arrival-route airport))
           (mapcat (fn [[route route-crafts]]
                     (space-crafts-along-route
                       engine route route-crafts))))))

  (next-arrival [_ {:keys [airport]}]
    (let [origin (pick-random
                   random
                   (-> airport :arrival-routes keys))]
      {:aircraft {:type :airline
                  :airline (pick-random random (keys all-airlines))
                  :flight-number (next-int random 20 9999)
                  :origin origin
                  :destination (:id airport)
                  :route (-> airport (get-in [:arrival-routes origin :route]))

                  ; TODO weather; runway selection
                  :runway (-> airport :runways first :start-id)
                  :config configs/common-jet}

       ; TODO Maybe depend on "difficulty"?
       :delay-to-next-s 240}))

  (next-departure [_ {:keys [airport]}]
    (let [destination (pick-random
                        random
                        (-> airport :departure-routes keys))]
      {:aircraft {:type :airline
                  :airline (pick-random random (keys all-airlines))
                  :flight-number (next-int random 20 9999)
                  :destination destination
                  :route (-> airport (get-in [:departure-routes destination]))

                  ; TODO weather; runway selection
                  :runway (-> airport :runways first :start-id)
                  :config configs/common-jet}

       ; TODO This should at least depend on the spawned aircraft's speed, etc. Maybe "difficulty"?
       :delay-to-next-s 240})))
