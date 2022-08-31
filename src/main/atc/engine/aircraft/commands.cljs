(ns atc.engine.aircraft.commands
  "Responding to commands"
  (:require
   [atc.data.airports :as airports]
   [atc.data.units :refer [nm->m]]
   [atc.engine.model :refer [angle-down-to bearing-to distance-to-squared]]
   [clojure.math :refer [pow]]))

(defn- normalize-heading [h]
  (if (< h 0)
    (+ h 360)
    (mod h 360)))


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

(defn- apply-direct [aircraft commanded-to dt]
  ; NOTE: This is temporary; the real logic should account for resuming course,
  ; intercept heading, crossing altitude, etc.
  (let [bearing-to-destination (bearing-to (:position aircraft) commanded-to)]
    (apply-steering aircraft bearing-to-destination dt)))


; ======= Approach course following =======================

; NOTE: We use squared distances to avoid having to compute sqrt
(def ^:private localizer-narrow-distance-m2 (pow (nm->m 18) 2.))
(def ^:private localizer-narrow-angle-degrees 10)
(def ^:private localizer-wide-distance-m2 (pow (nm->m 10) 2.))
(def ^:private localizer-wide-angle-degrees 35)

(def ^:private glide-slope-angle-degrees 3)
(def ^:private glide-slope-width-degrees 1.4)

(defn- within-localizer?
  "Returns the runway threshold if within its localizer, else nil"
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
        start))))

(defn- apply-ils-approach [aircraft {:keys [airport runway]} dt]
  (if-some [runway-start (within-localizer? aircraft airport runway)]
    ; TODO: If we just become established on the localizer and are above the glide slope,
    ;  that's no good. 
    (let [angle-to-runway (angle-down-to (:position aircraft) runway-start)]
      (when (>= (- glide-slope-angle-degrees
                   glide-slope-width-degrees)
                angle-to-runway
                (+ glide-slope-angle-degrees
                   glide-slope-width-degrees))
        ; TODO: Follow glide slope
        (println "On Glide slope!"))

      (-> aircraft

          ; We no longer need to maintain any specific heading
          (update :commands dissoc :heading :steer-direction)

          ; Steer toward the airport
          ; TODO: Perhaps, steer toward runway threshold? Or, maintain its heading? There's
          ; definitely a more realistic approach than either of these, but this may be
          ; sufficient for now...
          ; TODO: Detect "landing"
          (apply-direct airport dt)))

      ; Just proceed on course
      aircraft))

; I suppose this could be a defmulti...
(defn- apply-approach [aircraft {:keys [approach-type] :as command} dt]
  (case approach-type
    :ils (apply-ils-approach aircraft command dt)))


; ======= Publc interface =================================

(defn apply-commanded-inputs [aircraft, commands dt]
  (cond-> aircraft
    (:heading commands) (apply-steering (:heading commands) dt)
    (:direct commands) (apply-direct (:direct commands) dt)
    (:cleared-approach commands) (apply-approach (:cleared-approach commands) dt)))
