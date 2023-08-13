(ns atc.game.traffic.random
  (:require
   [atc.data.aircraft-configs :as configs]
   [atc.data.airlines :refer [all-airlines]]
   [atc.game.traffic.model :refer [ITraffic]]
   [atc.util.seedable :refer [next-int pick-random]]))

(defrecord RandomTraffic [random]
  ITraffic
  (next-departure [_ {:keys [airport]}]
    {:aircraft {:type :airline
                :airline (pick-random random (keys all-airlines))
                :flight-number (next-int random 20 9999)
                :destination (pick-random
                               random
                               (-> airport :departure-routes keys))

                ; TODO weather; runway selection
                :runway (-> airport :runways first :start-id)
                :config configs/common-jet}
     :delay-to-next-s 100}))
