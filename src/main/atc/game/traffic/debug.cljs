(ns atc.game.traffic.debug
  (:require
   [atc.data.aircraft-configs :as configs]
   [atc.data.units :refer [ft->m nm->m]]
   [atc.engine.model :refer [spawn-aircraft vec3]]
   [atc.game.traffic.model :refer [ITraffic]]))

(defrecord DebugTraffic []
  ITraffic
  (spawn-initial-arrivals [_ engine]
    (let [engine' (-> engine
                      (spawn-aircraft
                        {:type :airline
                         :airline "DAL"
                         :flight-number 22
                         :config configs/common-jet
                         :destination "KJFK"
                         :heading 300
                         :position (vec3
                                     (nm->m 10)
                                     (nm->m 10)
                                     (ft->m 4000))}))]
      {:engine engine'
       :delay-to-next-s (* 24 3600)}))

  (next-arrival [_ _]
    {:delay-to-next-s (* 24 3600)})
  (next-departure [_ _]
    {:delay-to-next-s (* 24 3600)}))
