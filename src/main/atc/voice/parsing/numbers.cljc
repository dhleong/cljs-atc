(ns atc.voice.parsing.numbers
  (:require
   [atc.voice.parsing.core :refer [declare-alternates]]))

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

(def rules
  ["frequency = (number-sequence whitespace)? decimal whitespace number-sequence"
   "heading = number-sequence"
   "<altitude> = flight-level | altitude-thousands-feet"
   "altitude-thousands-feet = digits-number whitespace (<'thousand'> | <'thousands'>)"
   "flight-level = <'flight level'> whitespace heading"

   "<decimal> = 'point' | 'decimal'"
   "digits-number = number-sequence"
   "number-sequence = number (whitespace number)*"
   "number = digit | double-digit"
   "digit-sequence = digit (whitespace digit)*"
   "double-digit = tens-value whitespace digit"
   (declare-alternates "digit" (keys digit-values))
   (declare-alternates "tens-value" (keys tens-values))
   ])

(defn digits->number [digits]
  (loop [numbers (reverse digits)
         multiplier 1
         result 0]
    (if (empty? numbers)
      result
      (recur
        (next numbers)
        (* multiplier 10)
        (+ result (* multiplier (first numbers)))))))

(def transformers
  {:digit digit-values
   :tens-value tens-values
   :double-digit (fn [tens ones]
                   (+ tens ones))
   :number identity
   :number-sequence (fn [& values] values)

   :heading (fn [values]
              (digits->number (take-last 3 values)))

   :digits-number digits->number
   :altitude-thousands-feet (fn [thousands]
                              (* 1000 thousands))
   :flight-level (fn [hundreds]
                   (* 100 hundreds))})
