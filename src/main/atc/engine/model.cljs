(ns atc.engine.model)

(defprotocol Simulated
  "Anything that is managed by the simulation"

  (tick [this dt]
        "Update this simulated item by [dt] ms"))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defrecord Vec3 [x y z])
