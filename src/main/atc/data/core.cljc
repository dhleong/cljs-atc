(ns atc.data.core
  (:require
   [clojure.math :refer [atan2 cos pow sin sqrt to-radians]]
   [clojure.string :as str]))

(def ^:private deg->rad to-radians)

(defprotocol Angle
  (coord-degrees [this]))

(defprotocol Coordinate
  (latlng [this] "Unpack this as a vector of lat, lng")
  (local-xy [this reference]
            "`reference` must also be a Coordinate of Angles"))

(defn- latlng-degrees [coord]
  (let [[lat lng] (latlng coord)]
    [(coord-degrees lat) (coord-degrees lng)]))

(defn- ->float [^String v]
  #?(:clj (Double/parseDouble v)
     :cljs (js/parseFloat v 10)))

(extend-protocol Angle
  #?(:clj java.lang.Number
     :cljs number)
  (coord-degrees [this] this)

  #?(:clj clojure.lang.Keyword
      :cljs cljs.core/Keyword)
  (coord-degrees [this]
    ; eg: :N42*10'32 or :N42.12345
    (when-let [match (str/split (name this) #"[nsewNSEW*']")]
      (let [[_ d m s] match
            sign (case (first (name this))
                   (\n \N \e \E) 1
                   (\s \S \w \W) -1)
            m (if-not m 0 (/ (->float m) 60))
            s (if-not s 0 (/ (->float s) 3600))]
        (* sign (+ (->float d) m s))))))

(def ^:private earth-radius-m 6371000)

(defn coord-distance
  "Returns the distance in meters from `from` to `to`, via the Haversine formula"
  [from to]
  (let [[flat flng] (latlng-degrees from)
        [tlat tlng] (latlng-degrees to)

        flat-radians (deg->rad flat)
        tlat-radians (deg->rad tlat)
        dlat (deg->rad (- flat tlat))
        dlng (deg->rad (- flng tlng))

        a (+ (pow (sin (/ dlat 2)) 2.)
             (* (cos flat-radians) (cos tlat-radians)
                (pow (sin (/ dlng 2)) 2.)))

        angular-radians (atan2
                          (sqrt a)
                          (sqrt (- 1 a)))]
    (* angular-radians 2 earth-radius-m)))

(defn- adjust-by-magnetic-north [x y magnetic-north]
  (let [th (- (atan2 y x) (deg->rad magnetic-north))
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
        (if (< ref-lng lng) (- abs-x) abs-x)
        (if (< ref-lat lat) (- abs-y) abs-y)
        (:magnetic-north reference 0)))))
