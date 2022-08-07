(ns atc.voice.parsing.airport
  (:require
   [atc.voice.parsing.core :refer [declare-alternates]]))

(defn generate-parsing-context [airport]
  (let [navaids-by-pronunciation (reduce
                                   (fn [m {:keys [pronunciation id]}]
                                     (assoc m pronunciation id))
                                   {}
                                   (:navaids airport))]
    {:rules [(declare-alternates "navaids" (map :pronunciation (:navaids airport)))]
     :instructions {:navaids navaids-by-pronunciation}}))
