(ns atc.util.numbers
  (:require
   [clojure.math :refer [floor]]
   #? (:clj [clojure.string :as str])))

(defn ->int [^String v]
  #? (:cljs (js/parseInt v 10)
      :clj (Integer/parseInt (str/replace v #"[^0-9]+" ""))))

(defn round-to-hundreds [n]
  (-> n
      (/ 100)
      (floor)
      (* 100)))
