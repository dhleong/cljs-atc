(ns atc.voice.parsing.instructions
  (:require
   [atc.util.instaparse :refer-macros [defalternates
                                       defalternates-expr
                                       defrules]]
   [atc.voice.parsing.instructions.visual-approach :refer [visual-approach-instruction-rules
                                                           visual-approach-rules]]
   [clojure.string :as str]))

(defalternates other-position
  ["center"
   "tower"
   "ground"])

(defalternates pleasantry
  ["good day"])

(defrules ^:private instructions-rules
  ["standby = <'standby'>"
   "disregard = <'disregard'>"
   "radar-contact = <'radar contact'>"

   "adjust-altitude = (<'climb'> | <'descend'>)? <'and'>? <'maintain'> altitude"

   "expect-runway = <'expect'> approach-type? <'runway'> runway"
   "cleared-approach = <'cleared'> approach-type <'approach'>? <'runway'>? runway <'approach'>?"
   "cancel-approach = <'cancel'> <'approach clearance'>"

   "contact-other = <'contact'> other-position frequency? pleasantry?"

   "direct = <'proceed direct'> navaid"

   "steer = (<'fly'> | 'turn right' | 'turn left') <'heading'> heading"

   "verify-atis = (<'verify you have'> atis? <'information'>? letter) | (atis? <'information'> letter <'is current'>)"]

  [:other-position :pleasantry :frequency :navaid :altitude
   :approach-type :runway :heading :atis :letter])

(defrules ^:private global-command-rules
  ["atis-update = attention-all atis? <'information'>? letter <'is current'>"]

  [:attention-all :atis :letter])

(defalternates-expr ^:hide-tag instruction
  (concat
    (->> instructions-rules
         :grammar
         keys)
    (->> visual-approach-instruction-rules
         :grammar
         keys)))

(defalternates-expr global-command
  (->> global-command-rules
       :grammar
       keys))

(defrules ^:private core-rules
  ["command = aircraft-command | global-command"

   "aircraft-command = callsign instruction+"

   "<attention-all> = <'attention'> <'all'>? <'aircraft'>? <'on frequency'>?"
   "<atis> = <'ay tis'>"
   "approach-type = 'i l s' | 'r nav' | 'visual'"
   "runway = number-sequence (letter | 'left' | 'right' | 'north' | 'south')?"]
  {:other-position other-position
   :pleasantry pleasantry
   :callsign nil
   :instruction instruction
   :global-command global-command
   :number-sequence nil
   :letter nil})

(def rules (merge-with merge
                       core-rules
                       instructions-rules
                       visual-approach-rules
                       global-command-rules))

(def transformers
  {:command (fn [v] v)

   :aircraft-command (fn [callsign & instructions]
                       {:callsign (second callsign)
                        :instructions instructions})

   :global-command (fn [command]
                     {:global? true
                      :instructions [command]})

   :contact-other (fn [other-position & etc]
                    [:contact-other
                     other-position
                     {:frequency (when (= :frequency (ffirst etc))
                                   (second (first etc)))
                      :pleasant? (= :pleasantry (first (last etc)))}])

   :expect-runway (fn expect-runway
                    ([runway] (expect-runway nil runway))
                    ([approach-type runway]
                     [:expect-runway runway {:approach-type approach-type}]))

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
