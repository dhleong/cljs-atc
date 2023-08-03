(ns atc.radio
  (:require
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
      [(first s)
       (second s)
       "thousand"])

    :else
    (-> v str)))

(defmethod process-speakable :heading
  [[_ v]]
  ; TODO 040 might come in as the int 40
  (-> v str seq))

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
