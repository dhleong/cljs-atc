(ns atc.voice.parsing.airport
  (:require
   [atc.voice.parsing.core :refer [declare-alternates]]
   [clojure.string :as str]))

(defn generate-parsing-context [airport]
  (let [navaids-by-pronunciation (reduce
                                   (fn [m {:keys [pronunciation name id]}]
                                     (assoc m
                                            (or pronunciation
                                                (when name
                                                  (str/lower-case name))
                                                (str/lower-case id))
                                            id))
                                   {}
                                   (:navaids airport))]
    {:rules [(declare-alternates "navaid-pronounced" (keys navaids-by-pronunciation))]
     :transformers {:navaid-pronounced navaids-by-pronunciation}}))
