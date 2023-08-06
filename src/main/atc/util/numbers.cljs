(ns atc.util.numbers)

(defn ->int [^String v]
  (js/parseInt v 10))
