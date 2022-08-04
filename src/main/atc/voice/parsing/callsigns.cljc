(ns atc.voice.parsing.callsigns 
  (:require
   [atc.voice.parsing.core :refer [declare-alternates]]))

(def airlines
  {"delta" "DAL"
   "speed bird" "BAW"})

(def plane-types
  ["piper"])

(def rules
  ["callsign = airline-callsign | civil-callsign"
   "airline-callsign = airline-names whitespace number-sequence"
   "civil-callsign = <('november' | plane-type)> whitespace number-sequence" ; TODO letter
   (declare-alternates "<plane-type>" plane-types)
   (declare-alternates "airline-names" (keys airlines))])

(def transformers
  {:airline-names airlines
   :airline-callsign (fn [airline numbers]
                       (apply str airline numbers))
   :civil-callsign (fn [numbers] ; TODO letter
                     (apply str "N" numbers))})
