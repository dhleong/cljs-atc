(ns atc.nasr.cdr
  (:require
   [atc.okay :as okay]
   [clojure.string :as str]))

(defn find-departure-routes [in icao]
  (->> in
       (okay/newlines-sequence)
       (into
         []
         (comp
           (map #(str/split % #","))
           (map (fn [[route-code origin destination departure-fix route-string artcc]]
                  {:route-code route-code
                   :origin origin
                   :destination destination
                   :departure-fix departure-fix
                   :route-string route-string
                   :artcc artcc}))
           (filter #(= icao (:origin %)))))))
