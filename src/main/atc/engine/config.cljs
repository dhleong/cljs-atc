(ns atc.engine.config)

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defrecord AircraftConfig [turn-rate])

(defn create [kvs]
  (map->AircraftConfig kvs))

