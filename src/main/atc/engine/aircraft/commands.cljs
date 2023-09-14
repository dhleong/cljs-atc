(ns atc.engine.aircraft.commands
  "Responding to commands"
  (:require
   [atc.data.airports :as airports]
   [atc.data.units :refer [nm->m]]
   [atc.engine.aircraft.commands.altitude :refer [apply-altitude]]
   [atc.engine.aircraft.commands.direct :refer [apply-direct]]
   [atc.engine.aircraft.commands.helpers :refer [normalize-heading]]
   [atc.engine.aircraft.commands.speed :refer [apply-target-speed]]
   [atc.engine.aircraft.commands.steering :refer [apply-steering]]
   [atc.engine.aircraft.commands.visual-approach
    :refer [apply-report-field-in-sight apply-visual-approach]]
   [atc.engine.model :refer [angle-down-to bearing-to distance-to-squared]]
   [clojure.math :refer [pow]]))


; ======= Approach course following =======================

; NOTE: We use squared distances to avoid having to compute sqrt
(def ^:private localizer-narrow-distance-m2 (pow (nm->m 18) 2.))
(def ^:private localizer-narrow-angle-degrees 10)
(def ^:private localizer-wide-distance-m2 (pow (nm->m 10) 2.))
(def ^:private localizer-wide-angle-degrees 35)

(def ^:private glide-slope-angle-degrees 3)
(def ^:private glide-slope-width-degrees 1.4)

(def ^:private landed-distance-m 5)

(defn- within-localizer?
  "Returns the [runway-threshold distance2] if within the runway's localizer, else nil"
  [aircraft airport runway]
  (when-let [[start end] (airports/runway-coords airport runway)]
    (let [runway-heading (normalize-heading (bearing-to start end))
          angle-to-threshold (normalize-heading (bearing-to (:position aircraft) start))
          delta (abs (- runway-heading angle-to-threshold))
          distance-to-runway2 (distance-to-squared (:position aircraft) start)]
      (when (cond
              (<= distance-to-runway2 localizer-wide-distance-m2)
              (<= delta localizer-wide-angle-degrees)

              (<= distance-to-runway2 localizer-narrow-distance-m2)
              (<= delta localizer-narrow-angle-degrees))
        [start distance-to-runway2]))))

(defn- apply-ils-approach [aircraft {:keys [airport runway]} dt]
  (if-some [[runway-start distance-to-runway2] (within-localizer? aircraft airport runway)]
    ; TODO: If we just become established on the localizer and are above the glide slope,
    ;  that's no good. 
    (cond-> aircraft
      ; Detect "landing"
      (<= distance-to-runway2
          landed-distance-m)
      (assoc :state :landed
             :commands {})

      ; Follow glide slope
      (>= (- glide-slope-angle-degrees
             glide-slope-width-degrees)
          (angle-down-to (:position aircraft) runway-start)
          (+ glide-slope-angle-degrees
             glide-slope-width-degrees))
      (->
        ; Ensure there's no competing altitude
        (update :commands dissoc :target-altitude)

        ; TODO Slow down

        ; Descend toward runway
        (apply-altitude (:z runway-start) dt))

      ; Always ensure we turn onto course while within the localizer
      :always
      (->
        ; We no longer need to maintain any specific heading
        (update :commands dissoc :heading :steer-direction)

        ; Steer toward the airport
        ; TODO: Perhaps, steer toward runway threshold? Or, maintain its heading? There's
        ; definitely a more realistic approach than either of these, but this may be
        ; sufficient for now...
        (apply-direct airport dt)))

      ; Just proceed on course, awaiting the intercept
      aircraft))

; I suppose this could be a defmulti...
(defn- apply-approach [aircraft {:keys [approach-type] :as command} dt]
  (case approach-type
    :ils (apply-ils-approach aircraft command dt)
    :visual (apply-visual-approach aircraft command dt)))


; ======= Public interface ================================

(defn apply-commanded-inputs [aircraft, commands dt]
  (cond-> aircraft
    (:heading commands) (apply-steering (:heading commands) dt)
    (:direct commands) (apply-direct (:direct commands) dt)
    (:cleared-approach commands) (apply-approach (:cleared-approach commands) dt)
    (:report-field-in-sight commands) (apply-report-field-in-sight
                                        (:report-field-in-sight commands)
                                        dt)
    (:target-altitude commands) (apply-altitude (:target-altitude commands) dt)
    (:target-speed commands) (apply-target-speed (:target-speed commands) dt)))
