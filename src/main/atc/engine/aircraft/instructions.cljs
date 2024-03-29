(ns atc.engine.aircraft.instructions
  (:require
   [atc.data.units :as units]
   [atc.engine.aircraft.commands.helpers :refer [primary-airport-position]]
   [atc.engine.aircraft.commands.visual-approach :refer [can-see-airport?]]
   [atc.config :as config]))

(defn- utter [craft & utterance]
  (update craft ::utterance-parts conj utterance))

(defmulti dispatch-instruction (fn [_craft _engine [instruction]] instruction))

(defmethod dispatch-instruction
  :adjust-altitude
  [{craft-pos :position :as craft} _ [_ altitude]]
  ; TODO Check state; if in approach, we should reject
  (let [altitude-m (units/ft->m altitude)]
    (-> craft
        (update :commands assoc :target-altitude altitude-m)
        (update :altitude-assignments
                (fnil conj [])
                {:direction (if (> altitude-m (:z craft-pos))
                              :climb
                              :descend)
                 :altitude-ft altitude})
        (utter "maintain" [:altitude altitude])

        (cond->
          ; If we were commanded to below 10K, ensure that we slow down to meet the "speed limit"
          (<= altitude 10000) (update-in [:commands :target-speed]
                                         min
                                         config/speed-limit-under-10k-kts)))))

(defmethod dispatch-instruction
  :cleared-approach
  [craft context [_ approach-type runway]]
  (cond
    (not (contains? (:game/airport-runway-ids context) runway))
    (utter craft "unable" runway)

    (= :rnav approach-type)
    (utter craft "unable" (name approach-type))

    ; TODO Actually you shouldn't clear for visual approach
    ; unless they've reported the field in sight
    (and (= :visual approach-type)
         (not (can-see-airport?
                craft
                (primary-airport-position (:airport context))
                (:weather context))))
    (utter craft "unable visual approach; I can't see the field yet")

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
  :report-field-in-sight
  [craft {:keys [airport weather]} _instruction]
  (if (can-see-airport? craft airport weather)
    (-> craft
        (utter "field in sight"))

    (-> craft
        (utter "looking for the field")
        ; NOTE: Eventually we might support destinations other than
        ; the primary airport; in that case we just provide the
        ; relevant airport's relative position
        (update :commands assoc :report-field-in-sight
                {:airport-position (primary-airport-position airport)}))))

(defmethod dispatch-instruction
  :verify-atis
  [craft _context [_ letter]]
  ; TODO: This is a bit of laziness; a more complete simulation might
  ; have some aircraft who have an *old* ATIS (or haven't gotten any)
  ; and need to a) go get it, and b) report back when they have it
  (-> craft
      (utter "we have " [:letter letter])
      (update :behavior assoc :will-get-weather? true)))

(defmethod dispatch-instruction
  :default
  [craft _context [instruction]]
  (println "TODO: Unhandled craft instruction: " instruction)
  craft)
