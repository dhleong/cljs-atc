(ns atc.voice.parsing.airport
  (:require
   [clojure.string :as str]))

(defn airport->navaids-by-pronunciation [airport]
  (reduce
    (fn [m {:keys [pronunciation name id]}]
      (assoc m
             (or pronunciation
                 (when name
                   (str/lower-case name))
                 (str/lower-case id))
             id))
    {}
    (:navaids airport)))
