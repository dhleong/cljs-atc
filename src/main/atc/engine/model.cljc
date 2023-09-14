(ns atc.engine.model
  (:require
   [clojure.math :refer [atan2 sqrt to-degrees]]))

(defprotocol Simulated
  "Anything that is managed by the simulation"

  (tick [this dt]
        "Update this simulated item by [dt] seconds")

  (command [this context instruction]
           "Process an instruction, of the form [:instruction ...args]"))

(defprotocol ICommunicator
  (pending-communication [this])
  (prepare-pending-communication [this])
  (consume-pending-communication [this]))

(defprotocol IGameEngine
  (spawn-aircraft
    [this opts]
    ; TODO support GA craft
    "opts is a map with:
     :type :airline/:ga
     :origin str        ; icao of origin airport
     :destination str   ; icao of destination airport
     :config map        ; see aircraft.configs
     :runway str        ; an active runway ID, to start on ground

     ; for airlines:
     :airline airline-id
     :flight-number int

     ; for ga:
     :plane-type str
     :tail-number str
     "))


#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defprotocol Vector
  (v+ [this other])
  (v- [this other])
  (v* [this other])
  (dot* [this other])
  (vmag2 [this] "Compute the square of the magnitude of this vector"))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defrecord Vec3 [x y z]
  Vector
  (v+ [this other]
    (assoc this
           :x (+ x (:x other))
           :y (+ y (:y other))
           :z (+ (or z 0) (:z other 0))))

  (v- [this other]
    (assoc this
           :x (- x (:x other))
           :y (- y (:y other))
           :z (- (or z 0) (:z other 0))))

  (v* [this other]
    (if (number? other)
      (assoc this
             :x (* x other)
             :y (* y other)
             :z (when z (* z other)))

      (assoc this
             :x (* x (:x other))
             :y (* y (:y other))
             :z (* (or z 1) (:z other 1)))))

  (dot* [this other]
    (+ (* (:x this) (:x other))
       (* (:y this) (:y other))
       (* (:z this 0) (:z other 0))))

  (vmag2 [this]
    (let [{dx :x dy :y dz :z} this]
      (+ (* dx dx)
         (* dy dy)
         (* dz dz)))))

(defn vmag [v]
  (sqrt (vmag2 v)))

(def vec3? (partial instance? Vec3))

(defn vec3
  ([v] (if (vec3? v) v
         (->Vec3 (:x v) (:y v) (:z v))))
  ([v z] (let [{:keys [x y]} (vec3 v)]
           (->Vec3 x y z)))
  ([x y z]
   (->Vec3 x y z)))

(defn normalize [v]
  (let [magnitude (vmag v)]
    (v* v (/ 1 magnitude))))

(defn lateral-distance-to-squared [from to]
  (vmag2 (v- (vec3 to 0) (vec3 from 0))))

(defn distance-to-squared [from to]
  (vmag2 (v- (vec3 to) from)))

(defn bearing-to->vec [from to]
  (v- (vec3 to) from))

(defn bearing-vec->degrees [{dx :x dy :y}]
  ; NOTE: This may or may not be the right move, but We want 0 degrees to
  ; point "north" on the screen, and so transform that in the Aircraft engine
  ; object with `(- heading 90)`, which means we have to do the opposite
  ; here....
  (+ (to-degrees (atan2 dy dx)) 90))

(defn bearing-to [from to]
  (let [bearing-vec (bearing-to->vec from to)]
    (bearing-vec->degrees bearing-vec)))

(defn angle-down-to
  "Assuming `from` is an elevated position and `to` is a position on the ground,
  return the angle between the ground and a line segment between `from` and `to`"
  [from to]
  (let [from (vec3 from)
        to (vec3 to)
        elevation (- (:z from) (:z to))]
    (to-degrees
      ; NOTE: Rather than involve any sqrts we just square elevation
      (atan2 (* elevation elevation)
             (vmag2
               (v- (vec3 from 0)
                   (vec3 to 0)))))))
