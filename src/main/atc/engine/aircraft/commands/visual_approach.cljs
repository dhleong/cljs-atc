(ns atc.engine.aircraft.commands.visual-approach
  (:require
   [atc.data.airports :as airports :refer [runway->heading runway-coords]]
   [atc.data.units :refer [ft->m nm->m sm->m]]
   [atc.engine.aircraft.commands.speed :refer [apply-target-speed]]
   [atc.engine.aircraft.commands.altitude :refer [apply-altitude]]
   [atc.engine.aircraft.commands.direct :refer [apply-direct]]
   [atc.engine.aircraft.commands.helpers :refer [normalize-heading utter-once]]
   [atc.engine.aircraft.commands.steering :refer [apply-steering]]
   [atc.engine.model :refer [angle-down-to bearing-to bearing-to->vec
                             distance-to-squared normalize v* vmag]]
   [atc.util.math :refer [squared]]))


; ======= Report field in sight ===========================

(defn can-see-airport? [craft airport-position {:keys [visibility-sm]}]
  (or (nil? visibility-sm)
      (let [visibility-m (sm->m visibility-sm)
            distance-sq (distance-to-squared (:position craft) airport-position)]
        (<= distance-sq (squared visibility-m)))))

(defn apply-report-field-in-sight [craft {:keys [airport-position weather]} _dt]
  (cond-> craft
    (can-see-airport? craft airport-position weather)
    (-> (update :commands dissoc :report-field-in-sight)
        (utter-once "field in sight"))))


; ======= Visual Approaches ===============================

(def ^:private heading-delta 15)

; NOTE: We accept a smaller delta here to avoid false positives. We could
; probably do some math to ensure we're positioned correctly relative to
; the runway threshold instead, but... this is faster for now.
(def ^:private base-heading-delta 5)

(defn compute-approach-leg [aircraft {:keys [airport runway]}]
  (let [[runway-threshold _] (runway-coords airport runway)
        runway-heading (airports/runway->heading airport runway)
        bearing-to-runway (bearing-to
                            (:position aircraft)
                            runway-threshold)
        downwind-heading (-> runway-heading
                             (+ 180)
                             (normalize-heading))
        left-base-heading (+ runway-heading 90)
        right-base-heading (+ runway-heading 90)]
    (cond
      (<= (- runway-heading heading-delta)
          bearing-to-runway
          (+ runway-heading heading-delta))
      :final

      (<= (- downwind-heading heading-delta)
          (:heading aircraft)
          (+ downwind-heading heading-delta))
      :downwind

      ; left base leg
      (<= (- left-base-heading base-heading-delta)
          (:heading aircraft)
          (+ left-base-heading base-heading-delta))
      :base

      ; right base leg
      (<= (- right-base-heading base-heading-delta)
          (:heading aircraft)
          (+ right-base-heading base-heading-delta))
      :base

      :else :enter-downwind)))

(defmulti ^:private apply-visual-approach-leg
  (fn [aircraft _engine _cmd _dt]
    (get-in aircraft [:behavior :visual-approach-state])))


; ======= Entering downwind ===============================

(def ^:private downwind-leg-distance-sq (squared (nm->m 5.5)))

(defmethod apply-visual-approach-leg :enter-downwind
  [aircraft _engine {:keys [airport _runway]} dt]
  (let [distance-sq (distance-to-squared
                      aircraft
                      (:position airport))]
    (if (<= distance-sq downwind-leg-distance-sq)
      (-> aircraft
          (update :behavior assoc :visual-approach-state :downwind))

      ; TODO enter at a 45 degree angle
      (-> aircraft
          (apply-direct (:position airport) dt)))))


; ======= Downwind leg ====================================

(defmethod apply-visual-approach-leg :downwind
  [aircraft _engine {:keys [airport runway]} dt]
  (let [target-heading (-> (runway->heading airport runway)
                           (+ 180)
                           (normalize-heading))
        target-altitude (ft->m (+ 1000 (last (:position airport))))
        [runway-threshold _] (runway-coords airport runway)
        bearing-to-aircraft (bearing-to runway-threshold (:position aircraft))
        can-turn-base? (and (not (get-in aircraft [:behavior :extend-downwind]))
                            (<= (- target-heading 45)
                                bearing-to-aircraft
                                (+ target-heading 45)))]
    (if can-turn-base?
      (-> aircraft
          (update :behavior assoc :visual-approach-state :base))

      (-> aircraft
          (apply-altitude target-altitude dt)
          (apply-target-speed (:landing-speed (:config aircraft)) dt)
          (apply-steering target-heading dt)))))


; ======= Base leg ========================================

(def ^:private turn-final-distance-sq-m (squared 500))

(defmethod apply-visual-approach-leg :base
  [aircraft _engine {:keys [airport runway]} dt]
  (let [[runway-threshold runway-end] (runway-coords airport runway)
        vector-to-aircraft (bearing-to->vec
                             (:position aircraft)
                             runway-threshold)
        distance-to-aircraft (vmag vector-to-aircraft)
        runway-vector (-> (bearing-to->vec
                            runway-threshold
                            runway-end)
                          (normalize))

        final-turn-position (v* runway-vector distance-to-aircraft)
        target-altitude (ft->m (+ 500 (last (:position airport))))

        can-turn-final? (<= (distance-to-squared
                              (:position aircraft)
                              final-turn-position)
                            turn-final-distance-sq-m)]
    (if can-turn-final?
      (-> aircraft
          (update :behavior assoc :visual-approach-state :final))

      (-> aircraft
          (apply-altitude target-altitude dt)
          (apply-target-speed (:landing-speed (:config aircraft)) dt)
          (apply-direct final-turn-position dt)))))


; ======= Final approach ==================================

(def ^:private landed-distance-m 5)
(def ^:private min-glide-slope-degrees 1.6)

(defmethod apply-visual-approach-leg :final
  [aircraft _engine {:keys [airport runway]} dt]
  (let [[runway-start _] (runway-coords airport runway)
        distance-to-runway2 (distance-to-squared
                              (:position aircraft) runway-start)]
    (cond-> aircraft
      ; Detect "landing"
      (<= distance-to-runway2
          landed-distance-m)
      (assoc :state :landed
             :commands {})

      ; Descend down to runway threshold
      (>= (angle-down-to (:position aircraft) runway-start)
          min-glide-slope-degrees)
      (->
        ; Ensure there's no competing altitude
        (update :commands dissoc :target-altitude)

        ; Slow down
        (apply-target-speed (:min-speed (:config aircraft)) dt)

        ; Descend toward runway
        (apply-altitude (:z runway-start) dt))

      ; Always ensure we turn onto course
      :always
      (->
        ; We no longer need to maintain any specific heading
        (update :commands dissoc :heading :steer-direction)

        ; Steer toward the runway threshold
        (apply-direct runway-start dt)))))


; ======= Primary entry point =============================

(defn apply-visual-approach [aircraft engine cmd dt]
  (let [leg (or (get-in aircraft [:behavior :visual-approach-state])
                (compute-approach-leg aircraft cmd))]
    (-> aircraft
        (assoc-in [:behavior :visual-approach-state] leg)
        (apply-visual-approach-leg engine cmd dt))))

