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

   "cleared-approach = <'cleared'> approach-type <'approach'>? <'runway'>? runway <'approach'>?"
   "cancel-approach = <'cancel approach clearance'>"

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

     "approach-type = 'i l s' | 'r nav' | 'visual'"
     "runway = number-sequence (letter | 'left' | 'right' | 'north' | 'south')?"
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

   :approach-type (fn [raw]
                    (keyword (str/replace raw #" " "")))

   :other-position keyword

   :runway (fn [numbers position]
             (str (str/join numbers)
                  (when position
                    (str/upper-case (first position)))))})
