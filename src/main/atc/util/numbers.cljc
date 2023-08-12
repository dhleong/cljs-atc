(ns atc.util.numbers
  (:require
   [clojure.math :refer [floor]]))

(defn ->int [^String v]
  #? (:cljs (js/parseInt v 10)
      :clj (Integer/parseInt v)))

(defn round-to-hundreds [n]
  (-> n
      (/ 100)
      (floor)
      (* 100)))
