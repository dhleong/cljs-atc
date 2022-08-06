(ns atc.voice.parsing.numbers
  (:require
   [atc.voice.parsing.core :refer [declare-alternates]]))

(def digit-values
  {"zero" 0
   "one" 1
   "two" 2
   "tree" 3
   "three" 3
   "four" 4
   "fife" 5
   "five" 5
   "six" 6
   "seven" 7
   "eight" 8
   "nine" 9
   "niner" 9})

(def teens-values
  {"eleven" 11
   "twelve" 12
   "thirteen" 13
   "fourteen" 14
   "fifteen" 15
   "sixteen" 16
   "seventeen" 17
   "eighteen" 18
   "nineteen" 19})

(def tens-values
  {"twenty" 20
   "thirty" 30
   "forty" 40
   "fifty" 50
   "sixty" 60
   "seventy" 70
   "eighty" 80
   "ninety" 90 })

(def rules
  ["frequency = number-sequence? decimal number-sequence"
   "heading = number-sequence"
   "<altitude> = flight-level | altitude-thousands-feet"
   "altitude-thousands-feet = digits-number (<'thousand'> | <'thousands'>)"
   "flight-level = <'flight level'> heading"

   "<decimal> = 'point' | 'decimal'"
   "digits-number = number-sequence"
   "number-sequence = number+"
   "number = digit | double-digit"
   "digit-sequence = digit+"
   "double-digit = (tens-value digit) | teens-value"
   (declare-alternates "digit" (keys digit-values))
   (declare-alternates "tens-value" (keys tens-values))
   (declare-alternates "teens-value" (keys teens-values))
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
   :teens-value teens-values
   :double-digit (fn [tens ones]
                   (if (some? ones)
                     (+ tens ones)
                     tens))
   :number identity
   :number-sequence (fn [& values] values)

   :heading (fn [values]
              (digits->number (take-last 3 values)))

   :digits-number digits->number
   :altitude-thousands-feet (fn [thousands]
                              (* 1000 thousands))
   :flight-level (fn [hundreds]
                   (* 100 hundreds))})
