(ns atc.util.numbers)

(defn ->int [^String v]
  #? (:cljs (js/parseInt v 10)
      :clj (Integer/parseInt v)))
