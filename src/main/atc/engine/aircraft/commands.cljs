(ns atc.engine.aircraft.commands
  "Responding to commands"
  (:require
   [atc.data.airports :as airports]
   [atc.data.units :refer [nm->m]]
   [atc.engine.aircraft.commands.visual-approach
    :refer [apply-report-field-in-sight]]
   [atc.engine.model :refer [angle-down-to bearing-to distance-to-squared]]
   [clojure.math :refer [pow]]))

(defn- normalize-heading [h]
  (if (< h 0)
    (+ h 360)
    (mod h 360)))

(defn- apply-rate [aircraft command-key path rate-keys from commanded-value dt]
  (let [sign (if (> commanded-value from) 1 -1)
        rate-key (rate-keys sign)
        rate (get-in aircraft [:config rate-key])
        new-speed (+ from (* sign rate dt))]
    (if (<= (abs (- commanded-value new-speed))
            (* rate 0.5))
      (-> aircraft
          (assoc-in path commanded-value)  ; close enough; snap to
          (update :commands dissoc command-key))

      (-> aircraft
          (assoc-in path new-speed)))))


; ======= Steering ========================================

(defn shorter-steer-direction [from to]
  ; With thanks to: https://math.stackexchange.com/a/2898118
  (let [delta (- (mod (- to from -540)
                      360)
                 180)]
    (if (>= delta 0)
      :right
      :left)))

(defn- apply-steering [{from :heading commands :commands :as aircraft}
                       commanded-to dt]
  (if (= from commanded-to)
    aircraft

    ; TODO at slower speeds, small craft might use a turn 2 rate (IE: 2x turn rate)
    ; TODO similarly, at higher speeds, large craft might use a half turn rate
    (let [turn-sign (case (or (:steer-direction commands)
                              (shorter-steer-direction from commanded-to))
                      :right  1
                      :left  -1)
          degrees-per-second (get-in aircraft [:config :turn-rate])
          turn-amount (* degrees-per-second dt)
          new-heading (normalize-heading (+ from (* turn-sign turn-amount)))]
      (if (<= (abs (- commanded-to new-heading))
              (* turn-amount 0.5))
        (-> aircraft
            (assoc :heading commanded-to)  ; close enough; snap to
            (update :commands dissoc :steer-direction))
        (assoc aircraft :heading new-heading)))))


; ======= Direct-to-point nav =============================

; This is the distance in meters squared at which an aircraft is considered
; to have "arrived" at its :direct target. We include quite a bit of slop
; to avoid excess steering, since you should maintain heading if you pass
; the point and don't have any other direction
(def ^:private over-coordinate-distance-m2 (pow 6500 2.))

(defn- apply-direct [aircraft commanded-to dt]
  ; NOTE: This is temporary; the real logic should account for resuming course,
  ; intercept heading, crossing altitude, etc.
  (let [distance2 (distance-to-squared (:position aircraft) commanded-to)]
    (if (< distance2 over-coordinate-distance-m2)
      ; We're "at" the destination; we can stop heading towards it now
      (update aircraft :commands dissoc :direct)

      (let [bearing-to-destination (bearing-to (:position aircraft) commanded-to)]
        (apply-steering aircraft bearing-to-destination dt)))))


; ======= Altitude ========================================

(defn- apply-altitude [{{from :z} :position :as aircraft} commanded-altitude dt]
  (if (or (= from commanded-altitude)
          ; The aircraft cannot climb if it's going too slow! There's a bit
          ; of an assumption here that the :landing-speed will never be less
          ; than this, but that seems... safe?
          (< (:speed aircraft) (:min-speed (:config aircraft))))
    aircraft

    (apply-rate
      aircraft
      :target-altitude [:position :z]
      {-1 :descent-rate
       1 :climb-rate}
      from commanded-altitude dt)))


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
    :ils (apply-ils-approach aircraft command dt)))


; ======= Speed ===========================================

(defn apply-target-speed [{from :speed :as aircraft} commanded-speed dt]
  (if (= from commanded-speed)
    aircraft

    (apply-rate
      aircraft
      :target-speed [:speed]
      {-1 :deceleration
       1 :acceleration}
      from commanded-speed dt)))


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
