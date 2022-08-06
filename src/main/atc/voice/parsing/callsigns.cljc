(ns atc.voice.parsing.callsigns 
  (:require
   [atc.voice.parsing.core :refer [declare-alternates]]))

(def airlines
  {"delta" "DAL"
   "speed bird" "BAW"})

(def plane-types
  ["piper"])

(def rules
  ["callsign = airline-callsign | ga-callsign"
   "airline-callsign = airline-names number-sequence"
   "ga-callsign = <('november' | plane-type)> number-sequence (letter-sequence)?"
   (declare-alternates "<plane-type>" plane-types)
   (declare-alternates "airline-names" (keys airlines))])

(def transformers
  {:airline-names airlines
   :airline-callsign (fn [airline numbers]
                       (apply str airline numbers))

   :ga-callsign (fn [numbers letters]
                  ; NOTE: per the FAA this *should* be:
                  ; N + 5 numbers,
                  ; N + 4 numbers and a letter, or
                  ; N + 3 numbers + 2 letters, AND
                  ; the first number must not be 0 AND
                  ; letters O and I should not be used
                  (apply str "N" (concat numbers letters)))})
