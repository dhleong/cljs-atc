(ns atc.engine.model
  (:require
   [clojure.math :refer [atan2 to-degrees]]))

(defprotocol Simulated
  "Anything that is managed by the simulation"

  (tick [this dt]
        "Update this simulated item by [dt] seconds")

  (command [this instruction]
           "Process an instruction, of the form [:instruction ...args]"))

(defprotocol ICommunicator
  (pending-communication [this])
  (prepare-pending-communication [this])
  (consume-pending-communication [this]))


#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defprotocol Vector
  (v+ [this ^Vector other])
  (v- [this ^Vector other])
  (v* [this other]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defrecord Vec3 [x y z]
  Vector
  (v+ [this ^Vec3 other]
    (assoc this
           :x (+ x (:x other))
           :y (+ y (:y other))
           :z (+ z (:z other 0))))

  (v- [this ^Vec3 other]
    (assoc this
           :x (- x (:x other))
           :y (- y (:y other))
           :z (- z (:z other 0))))

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

(defn vec3
  ([v] (if (instance? Vec3 v) v
         (->Vec3 (:x v) (:y v) (:z v))))
  ([x y z]
   (->Vec3 x y z)))

(defn bearing-to [from to]
  (let [{dx :x dy :y} (v- (vec3 to) from)]
    ; NOTE: This may or may not be the right move, but We want 0 degrees to
    ; point "north" on the screen, and so transform that in the Aircraft engine
    ; object with `(- heading 90)`, which means we have to do the opposite
    ; here....
    (+ (to-degrees (atan2 dy dx)) 90)))

