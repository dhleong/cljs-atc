(ns atc.voice.parsing.numbers
  (:require [clojure.string :as str]))

(def digit-values
  {"zero" 0
   "one" 1
   "two" 2
   "three" 3
   "four" 4
   "five" 5
   "six" 6
   "seven" 7
   "eight" 8
   "nine" 9})

(def tens-values
  {"twenty" 20
   "thirty" 30
   "fourty" 40
   "fifty" 50
   "sixty" 60
   "seventy" 70
   "eighty" 80
   "ninety" 90 })

(defn declare-alternates [rule-name values]
  (->> values
       (map #(str "'" % "'"))
       (str/join " | ")
       (str rule-name " = ")))

(def rules
  ["number-sequence = number (whitespace number)*"
   "number = digit | double-digit"
   "digit-sequence = digit (whitespace digit)*"
   "double-digit = tens-value whitespace digit"
   (declare-alternates "digit" (keys digit-values))
   (declare-alternates "tens-value" (keys tens-values))
   ])
