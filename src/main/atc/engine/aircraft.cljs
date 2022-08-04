(ns atc.engine.aircraft
  (:require
   [atc.engine.model :refer [->Vec3 Vec3 Simulated]]))

(defn- speed->mps [speed]
  (* 0.514444 speed))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defrecord Aircraft [config callsign
                     state
                     ^Vec3 position heading speed]
  Simulated
  (tick [this dt]
    (update-in this [:position :x] + (* (/ dt 1000)
                                        (speed->mps speed)))))

(defn create [config callsign]
  (map->Aircraft {:config config
                  :callsign callsign
                  :state :flight
                  :position (->Vec3 250 250 20000)
                  :heading 120
                  :speed 200}))
