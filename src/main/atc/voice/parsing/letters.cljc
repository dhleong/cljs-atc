(ns atc.voice.parsing.letters
  (:require
   [atc.util.instaparse :refer-macros [defalternates defrules]]))

(defalternates letter
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

(defrules rules
  "letter-sequence = letter+"
  {:letter letter})

(def transformers
  {:letter letter
   :letter-sequence (fn [& values] values)})
