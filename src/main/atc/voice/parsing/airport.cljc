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
    {:rules ["<navaid> = navaid-pronounced | navaid-spelled"
             "navaid-spelled = (letter letter letter <'v o r'>?) | (letter letter letter letter letter <'intersection'>?)"

             (declare-alternates "navaid-pronounced" (keys navaids-by-pronunciation))]
     :transformers {:navaid-pronounced navaids-by-pronunciation
                    :navaid-spelled (fn [& letters]
                                      (str/join letters))}}))
