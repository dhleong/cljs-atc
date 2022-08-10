(ns atc.radio)

(def ^:private choose-speakable
  (comp
    (keep (fn [item]
           (or
             (when (string? item) item)
             (:pronunciation item)
             (:radio-name item)

             (when-not (nil? item)
               (println "WARNING: " item "cannot be spoken")))))
    (interpose " ")))

(defn ->speakable [utterance]
  (if (string? utterance)
    utterance
    (->> utterance
         flatten
         (transduce choose-speakable str ""))))
