(ns atc.engine.model)

(defprotocol Simulated
  "Anything that is managed by the simulation"

  (tick [this dt]
        "Update this simulated item by [dt] ms"))

(defprotocol Vector
  (v+ [this ^Vector other])
  (v* [this other]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defrecord Vec3 [x y z]
  Vector
  (v+ [this ^Vec3 other]
    (assoc this
           :x (+ x (:x other))
           :y (+ y (:y other))
           :z (+ z (:z other 0))))

  (v* [this other]
    (if (number? other)
      (assoc this
             :x (* x other)
             :y (* y other)
             :z (* z other))

      (assoc this
             :x (* x (:x other))
             :y (* y (:y other))
             :z (* z (:z other 1))))))

(defn vec3 [x y z]
  (->Vec3 x y z))
