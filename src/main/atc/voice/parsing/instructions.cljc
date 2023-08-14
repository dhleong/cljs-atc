(ns atc.voice.parsing.instructions
  (:require
   [atc.util.instaparse :refer-macros [defalternates
                                       defalternates-expr
                                       defrules]]
   [clojure.string :as str]))

(defalternates other-position
  ["center"
   "tower"
   "ground"])

(defalternates pleasantry
  ["good day"])

(defrules ^:private instructions-rules
  ["standby = <'standby'>"

   "adjust-altitude = (<'climb'> | <'descend'>)? <'and'>? <'maintain'> altitude"

   "cleared-approach = <'cleared'> approach-type <'approach'>? <'runway'>? runway <'approach'>?"
   "cancel-approach = <'cancel'> <'approach clearance'>"

   "contact-other = <'contact'> other-position frequency? pleasantry?"

   "direct = <'proceed direct'> navaid"

   "steer = (<'fly'> | 'turn right' | 'turn left') <'heading'> heading"]

  [:other-position :pleasantry :frequency :navaid :altitude :approach-type :runway :heading])

(defalternates-expr ^:hide-tag instruction
  (->> instructions-rules
       :grammar
       keys
       (map name)))

(defrules rules
  ["command = callsign instruction+"

   "approach-type = 'i l s' | 'r nav' | 'visual'"
   "runway = number-sequence (letter | 'left' | 'right' | 'north' | 'south')?"]
  {:other-position other-position
   :pleasantry pleasantry
   :callsign nil
   :instruction instruction
   :number-sequence nil
   :letter nil})

(def transformers
  {:command (fn [callsign & instructions]
              {:callsign (second callsign)
               :instructions instructions})

   :contact-other (fn [other-position & etc]
                    [:contact-other
                     other-position
                     {:frequency (when (= :frequency (ffirst etc))
                                   (second (first etc)))
                      :pleasant? (= :pleasantry (first (last etc)))}])

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
