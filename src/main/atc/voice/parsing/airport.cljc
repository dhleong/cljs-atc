(ns atc.voice.parsing.airport
  (:require
   [atc.voice.parsing.core :refer [declare-alternates]]
   [clojure.string :as str]))

(defn generate-parsing-context [airport]
  (let [navaids-by-pronunciation (reduce
                                   (fn [m {:keys [pronunciation id]}]
                                     (assoc m
                                            (or pronunciation
                                                (str/lower-case id))
                                            id))
                                   {}
                                   (:navaids airport))]
    {:rules [(declare-alternates "navaid" (keys navaids-by-pronunciation))]
     :transformers {:navaid navaids-by-pronunciation}}))
