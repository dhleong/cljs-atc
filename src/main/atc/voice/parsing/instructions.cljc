(ns atc.voice.parsing.instructions 
  (:require
   [atc.voice.parsing.core :refer [declare-alternates]]))

(def handoffs
  ["center"
   "tower"
   "ground"])

(def pleasantries
  ["good day"])

(def rules
  ["command = callsign instruction+"
   "<instruction> = 'standby'
   | adjust-altitude
   | contact-other
   | steer"

   "adjust-altitude = (<'climb'> | <'descend'>)? <'and'>? <'maintain'> altitude"

   "contact-other = <'contact'> other-position <frequency>? pleasantry?"

   "steer = (<'fly'> | 'turn right' | 'turn left') <'heading'> heading"

   (declare-alternates "<other-position>" handoffs)
   (declare-alternates "pleasantry" pleasantries)])

(def transformers
  {:command (fn [callsign & instructions]
              {:callsign (second callsign)
               :instructions instructions})})
