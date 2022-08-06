(ns atc.voice.parsing.letters
  (:require
   [atc.voice.parsing.core :refer [declare-alternates]]))

(def letter-values
  {"alpha" "A"
   "bravo" "B"
   "charlie" "C"
   "delta" "D"
   "echo" "E"
   "foxtrot" "F"
   "golf" "G"
   "hotel" "H"
   "india" "I"
   "juliet" "J"
   "kilo" "K"
   "lima" "L"
   "mike" "M"
   "november" "N"
   "oscar" "O"
   "papa" "P"
   "quebec" "Q"
   "romeo" "R"
   "sierra" "S"
   "tango" "T"
   "uniform" "U"
   "victor" "V"
   "whiskey" "W"
   "x ray" "X"
   "yankee" "Y"
   "zulu" "Z"})

(def rules
  ["letter-sequence = letter+"
   (declare-alternates "letter" (keys letter-values))])

(def transformers
  {:letter letter-values
   :letter-sequence (fn [& values] values)})
