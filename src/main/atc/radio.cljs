(ns atc.radio
  (:require
   [atc.data.airlines :refer [all-airlines]]
   [atc.util.numbers :refer [->int]]
   [atc.voice.parsing.letters :refer [letter]]
   [atc.voice.parsing.numbers :as numbers]
   [clojure.set :refer [map-invert]]
   [com.rpl.specter :as s]))

; TODO: Perhaps we could introduce a Speakable protocol that PersistentVector can implement...

; TODO Convert numbers such that eg 9 -> niner, 3 -> tree, etc.

(defmulti ^:private process-speakable (fn [[kind _]] kind))

(defmethod process-speakable :altitude
  [[_ v]]
  ; 20000 -> 2 0 thousand
  (cond
    (>= v 10000)
    (let [s (str v)]
      [(process-speakable [:number (->int (first s))])
       (process-speakable [:number (->int (second s))])
       "thousand"])

    :else
    (-> v str)))

(defmethod process-speakable :approach-type
  [[_ v]]
  (case v
    :ils "I L S"
    :rnav "R nav"
    :visual "visual"))

(defmethod process-speakable :heading
  [[_ v]]
  ; TODO 040 might come in as the int 40
  (-> v str seq))

(def ^:private letter->speakable
  (map-invert letter))

(defmethod process-speakable :letter
 [[_ v]]
 (letter->speakable v))

(defmethod process-speakable :runway
  [[_ string]]
  (->> string
       seq
       (map (fn [v]
              (case v
                \L "left"
                \R "right"
                \N "north"
                \S "south"
                v)))))

(defmethod process-speakable :number
  [[_ v]]
  (cond
    (<= 0 v 9)
    (get (map-invert numbers/digit) v)

    (<= 10 v 19)
    (get (map-invert numbers/teens-value) v)

    :else v))

(defmethod process-speakable :group-form
  [[_ v]]
  ; NOTE: This is only really works for callsigns right now
  (cond
    ; 1/2 digits
    (< v 100)
    (process-speakable [:number v])

    ; 3/4 digits
    (< v 10000)
    (let [hundred-digits (js/Math.floor (/ v 100))]
      (str
        (process-speakable [:number hundred-digits])
        " "
        (process-speakable [:group-form/half (- v (* hundred-digits 100))])))

    ; TODO >4 digits
    :else v))

(defmethod process-speakable :group-form/half
  [[_ v]]
  (cond
    ; eg "zero niner"
    (< v 10)
    (str "zero "
         (process-speakable [:number v]))

    ; eg "ten" or "22"
    :else v))

(defmethod process-speakable :default [v]
  (println "WARNING: Unknown speakable kind format:" v))

(defn- speakable? [v]
  (and (vector? v)
       (keyword? (first v))
       (= 2 (count v))))

(defn- speakable-map->str [item]
  (or
    (:radio-name item)
    (:name item)
    (:pronunciation item)
    (:id item)

    (println "WARNING: " item "cannot be spoken")))

(defn- readable-map->str [item]
  (or
    (:id item)
    (:name item)
    (:radio-name item)
    (:pronunciation item)

    (println "WARNING: " item "cannot be read")))

(def ^:private create-speakable-chooser
  (memoize
    (fn [mode]
      (let [map->str (case mode
                       :speakable speakable-map->str
                       :readable readable-map->str)]
        (comp
          (keep
            (fn [item]
              (cond
                (string? item) item
                (nil? item) nil

                (map? item)
                (map->str item)

                :else
                item)))

          (interpose " "))))))

(def ^:private walk-speakables
  ; Might be nice to properly lint this macro:
  ; Specter is really hard for kondo to lint...
  #_{:clj-kondo/ignore [:unresolved-symbol :unresolved-var]}
  (s/recursive-path []
    p
    (s/cond-path
      speakable? s/STAY
      map? s/STOP
      sequential? [s/ALL p]
      :else s/STOP)))

(defn ->speakable [utterance]
  (if (string? utterance)
    utterance
    (->> utterance

         ; pre-process speakable values:
         (s/transform [walk-speakables] process-speakable)

         flatten
         (transduce (create-speakable-chooser :speakable) str ""))))

(defn ->readable [utterance]
  (if (string? utterance)
    utterance
    (->> utterance

         ; pre-process speakable values:
         (s/transform [walk-speakables] (fn [[_kind v]] v))

         flatten
         (transduce (create-speakable-chooser :readable) str ""))))

(defn format-airline-radio [airline-id flight-number]
  (let [radio-name (str
                     (get-in all-airlines [airline-id :radio-name])
                     " "
                     (->speakable [[:group-form flight-number]]))]
    {:callsign (str airline-id flight-number)
     :radio-name radio-name}))
