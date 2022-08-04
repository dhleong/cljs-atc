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
  ["command = callsign whitespace instruction (whitespace instruction)*"
   "<instruction> = 'standby'
   | adjust-altitude
   | contact-other
   | steer"

   "adjust-altitude =
   (<'climb'> | <'descend'>)?
   (whitespace <'and'>)? whitespace <'maintain'>
   whitespace altitude"

   "contact-other = <'contact'> whitespace other-position
   (whitespace <frequency>)?
   (whitespace pleasantry)?"

   "steer = (<'fly'> | 'turn right' | 'turn left') whitespace <'heading'> whitespace heading"

   (declare-alternates "<other-position>" handoffs)
   (declare-alternates "pleasantry" pleasantries)])

(def transformers
  {:command (fn [callsign & instructions]
              (println instructions)
              {:callsign (second callsign)
               :instructions instructions})})
