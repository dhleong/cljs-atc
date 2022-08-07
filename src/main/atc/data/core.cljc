(ns atc.data.core
  (:require
   [clojure.math :refer [atan2 cos PI pow sin sqrt]]
   [clojure.string :as str]))

(def deg->rad
  #?(:clj Math/toRadians
     :cljs (partial * (/ js/Math.PI 180)))
  )

(defprotocol Angle
  (coord-decimal [this])
  (coord-radians [this]))

(defprotocol Coordinate
  (latlng [this] "Unpack this as a vector of lat, lng")
  (local-xy [this reference]
            "`reference` must also be a Coordinate of Angles"))

(defn- ->float [^String v]
  #?(:clj (Double/parseDouble v)
     :cljs (js/parseFloat v 10)))

(extend-protocol Angle
  #?(:clj java.lang.Number
     :cljs number)
  (coord-decimal [this] this)
  (coord-radians [this] (deg->rad this))

  #?(:clj clojure.lang.Keyword
      :cljs cljs.core/Keyword)
  (coord-decimal [this]
    ; eg: :N42*10'32 or :N42.12345
    (when-let [match (str/split (name this) #"[nsewNSEW*']")]
      (let [[_ d m s] match
            sign (case (first (name this))
                   (\n \N \e \E) 1
                   (\s \S \w \W) -1)
            m (when m (/ (->float m) 60))
            s (when s (/ (->float s) 3600))]
        (* sign (+ (->float d) m s)))))
  (coord-radians [this]
    (deg->rad (coord-decimal this))))

(def ^:private earth-radius-m 6371000)

(defn coord-distance
  "Returns the distance in meters from `from` to `to`, via the Haversine formula"
  [^Coordinate from ^Coordinate to]
  (let [[flat flng] (latlng from)
        [tlat tlng] (latlng to)

        flat (coord-radians flat)
        flng (coord-radians flng)
        tlat (coord-radians tlat)
        tlng (coord-radians tlng)
        dlat (deg->rad (- flat tlat))
        dlng (deg->rad (- flng tlng))

        a (+ (pow (sin (/ dlat 2)) 2.)
             (* (cos flat) (cos tlat)
                (pow (sin (/ dlng 2)) 2.)))

        angular-radians (atan2
                          (sqrt a)
                          (sqrt (- 1 a)))]
    (* angular-radians 2 PI earth-radius-m)))

(defn- adjust-by-magnetic-north [x y magnetic-north]
  ; FIXME openscope adds magnetic north; why is this flipped?
  (let [th (- (atan2 y x) magnetic-north)
        r (sqrt (+ (* x x) (* y y)))]
    {:x (* r (cos th))
     :y (* r (sin th))}))

(extend-protocol Coordinate
  #?(:clj clojure.lang.PersistentVector
     :cljs cljs.core/PersistentVector)
  (latlng [this] this)
  (local-xy [[lat lng] reference]
    (let [ref-position (:position reference)
          [ref-lat ref-lng] (latlng ref-position)
          abs-x (coord-distance ref-position [ref-lat lng])
          abs-y (coord-distance ref-position [lat ref-lng])]
      (adjust-by-magnetic-north
        ; FIXME openscope checks lat for y and lng for x; why is this flipped?
        (if (> ref-lat lat) (- abs-x) abs-x)
        (if (> ref-lng lng) (- abs-y) abs-y)
        (:magnetic-north reference)))))
