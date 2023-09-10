(ns atc.engine.aircraft.instructions
  (:require
   [atc.data.units :as units]))

(defn- utter [craft & utterance]
  (update craft ::utterance-parts conj utterance))

(defmulti dispatch-instruction (fn [_craft _context [instruction]] instruction))

(defmethod dispatch-instruction
  :adjust-altitude
  [craft _ [_ altitude]]
  ; TODO Check state; if in approach, we should reject
  (-> craft
      (update :commands assoc :target-altitude (units/ft->m altitude))
      (utter "maintain" [:altitude altitude])))

(defmethod dispatch-instruction
  :cleared-approach
  [craft context [_ approach-type runway]]
  (cond
    (not (contains? (:game/airport-runway-ids context) runway))
    (utter craft "unable" runway)

    (contains? #{:visual :rnav} approach-type)
    (utter craft "unable" (name approach-type))

    :else (-> craft
            (assoc :state :cleared-approach)
            (update :commands dissoc :direct)
            (update :commands assoc :cleared-approach {:approach-type approach-type
                                                       :airport (:airport context)
                                                       :runway runway})
            (utter "cleared approach runway" [:runway runway]))))

(defmethod dispatch-instruction
  :expect-runway
  [craft context [_ runway {:keys [approach-type]}]]
  (cond
    (not (contains? (:game/airport-runway-ids context) runway))
    (utter craft "unable" runway)

    (contains? #{:visual :rnav} approach-type)
    (utter craft "unable" (name approach-type))

    ; TODO: Stash this in the flight strip
    :else (-> craft
              (utter "we'll expect"
                     (when approach-type "the")
                     (when approach-type
                       [:approach-type approach-type])
                     "runway" [:runway runway]))))

(defmethod dispatch-instruction
  :cancel-approach
  [craft _ _]
  (cond
    ; If not currently cleared, this is a nop:
    (not (get-in craft [:commands :cleared-approach]))
    craft

    :else (-> craft
            (assoc :state :flight)
            (update :commands dissoc :cleared-approach)
            (utter "cancel approach"))))

(defmethod dispatch-instruction
 :contact-other
 [craft {:keys [airport]} [_ other-position {:keys [frequency pleasant?]}]]
 ; TODO Check if the given frequency is valid
 (let [new-track (case other-position
                   :tower (get-in airport [:positions :twr])
                   :ground (get-in airport [:positions :gnd])
                   :center {:track-symbol "C"
                            ; TODO filter by provided frequency
                            :frequency (->> airport
                                            :center-facilities
                                            first
                                            :frequency)})]
   (-> craft
       (assoc :state :handing-off)
       (update :commands assoc :handoff-to {:position other-position
                                            :track new-track
                                            :frequency frequency})
       (utter "over to" (name other-position)
              (when pleasant?
                "good day")))))

(defmethod dispatch-instruction
  :steer
  [craft _ [_ heading steer-direction]]
  ; TODO Check state; if in approach, we should reject
  (-> craft
      (utter (when steer-direction (name steer-direction))
             [:heading heading])

      (update :commands dissoc :direct)
      (update :commands assoc :heading heading :steer-direction steer-direction)))

(defmethod dispatch-instruction
  :direct
  [craft context [_ fix-id]]
  ; TODO Check state; if in approach, we should reject
  (if-let [fix (get-in context [:game/navaids-by-id fix-id])]
    (-> craft
        (utter "direct " fix)
        (update :commands dissoc :heading)
        (update :commands assoc :direct fix))

    ; TODO: Handle GA aircraft without the fix
    (-> craft
        (utter "unable direct"))))


(defmethod dispatch-instruction
  :radar-contact
  [craft _context _instruction]
  ; Pilots don't need to do anything with this.
  craft)

(defmethod dispatch-instruction
  :verify-atis
  [craft _context [_ letter]]
  (-> craft
      (utter "we have " [:letter letter])
      (update :behavior assoc :will-get-weather? true)))

(defmethod dispatch-instruction
  :default
  [craft _context [instruction]]
  (println "TODO: Unhandled craft instruction: " instruction)
  craft)
