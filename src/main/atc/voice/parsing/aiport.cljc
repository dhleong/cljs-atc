(ns atc.voice.parsing.aiport
  (:require
   [atc.voice.parsing.core :refer [declare-alternates]]))

(defn generate-parsing [airport]
  (let [navaids-by-pronunciation (reduce
                                   (fn [m {:keys [pronunciation id]}]
                                     (assoc m pronunciation id))
                                   {}
                                   (:navaids airport))]
    {:rules [(declare-alternates "navaids" (map :pronunciation (:navaids airport)))]
     :instructions {:navaids navaids-by-pronunciation}}))
