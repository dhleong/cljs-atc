(ns atc.voice.parsing.callsigns
  (:require
   [atc.data.airlines :refer [all-airlines]]
   [atc.util.instaparse :refer-macros [defalternates
                                       defalternates-expr
                                       defrules]]))

(defalternates-expr airline-names
  (->> all-airlines
       (into {}
             (map (fn [[callsign {:keys [radio-name]}]]
                    [radio-name callsign])))))

(defalternates ^:hide-tag plane-type
  ["piper"])

(defrules rules
  ["callsign = airline-callsign | ga-callsign"
   "airline-callsign = airline-names number-sequence"
   "ga-callsign = <('november' | plane-type)> number-sequence (letter-sequence)?"]
  {:plane-type plane-type
   :airline-names airline-names
   :number-sequence nil
   :letter-sequence nil})

(def transformers
  {:airline-names airline-names
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
