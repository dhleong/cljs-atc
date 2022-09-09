(ns atc.radio
  (:require
   [com.rpl.specter :as s]))

; TODO: Perhaps we could introduce a Speakable protocol that PersistentVector can implement...

; TODO Convert numbers such that eg 9 -> niner, 3 -> tree, etc.

(defmulti ^:private process-speakable (fn [[kind _]] kind))

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

(defmethod process-speakable :heading
  [[_ v]]
  ; TODO 040 might come in as the int 40
  (-> v str seq))

(defmethod process-speakable :default [v]
  (println "WARNING: Unknown speakable kind format:" v))

(defn- speakable? [v]
  (and (vector? v)
       (keyword? (first v))
       (= 2 (count v))))

(def ^:private choose-speakable
  (comp
    (keep
      (fn [item]
        (cond
          (string? item) item
          (nil? item) nil

          (map? item)
          (or
            (:pronunciation item)
            (:radio-name item)
            (:id item)

            (println "WARNING: " item "cannot be spoken"))

          :else
          item)))

    (interpose " ")))

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
         (transduce choose-speakable str ""))))
