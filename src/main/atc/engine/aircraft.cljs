(ns atc.engine.aircraft
  (:require
   [atc.engine.model :refer [->Vec3 Vec3 Simulated]]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defrecord Aircraft [config callsign
                     state
                     ^Vec3 position heading speed]
  Simulated
  (tick [this _dt]
    ; TODO
    this))

(defn create [config callsign]
  (map->Aircraft {:config config
                  :callsign callsign
                  :state :flight
                  :position (->Vec3 250 250 20000)
                  :heading 120
                  :speed 200}))
