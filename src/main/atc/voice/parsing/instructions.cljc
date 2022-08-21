(ns atc.voice.parsing.instructions
  (:require
   [atc.voice.parsing.core :refer [declare-alternates]]
   [clojure.string :as str]))

(def handoffs
  ["center"
   "tower"
   "ground"])

(def pleasantries
  ["good day"])

(def ^:private instructions-rules
  ["standby = <'standby'>"

   "adjust-altitude = (<'climb'> | <'descend'>)? <'and'>? <'maintain'> altitude"

   "cleared-approach = <'cleared approach'> <'runway'>? runway"

   "contact-other = <'contact'> other-position <frequency>? pleasantry?"

   "direct = <'proceed direct'> navaid"

   "steer = (<'fly'> | 'turn right' | 'turn left') <'heading'> heading"])

(def rules
  (concat 
    ["command = callsign instruction+"
     (str "<instruction> = " (->> instructions-rules
                                  (map #(let [parts (str/split % #" = ")]
                                          (first parts)))
                                  (str/join " | ")))

     "runway = number+ (letter | 'left' | 'right' | 'north' | 'south')?"
     (declare-alternates "other-position" handoffs)
     (declare-alternates "pleasantry" pleasantries)]
    instructions-rules))

(def transformers
  {:command (fn [callsign & instructions]
              {:callsign (second callsign)
               :instructions instructions})

   :steer (fn [?direction ?heading]
            (if ?heading
              [:steer ?heading (case ?direction
                                 "turn right" :right
                                 "turn left" :left)]
              [:steer ?direction]))

   :other-position keyword

   :runway (fn [& parts]
             (let [numbers (butlast parts)
                   position (last parts)]
               (str (str/join numbers)
                    (str/upper-case (first position)))))})
