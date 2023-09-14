(ns atc.engine.aircraft.commands.visual-approach
  (:require
   [atc.data.airports :as airports :refer [runway->heading runway-coords]]
   [atc.data.units :refer [ft->m sm->m]]
   [atc.engine.aircraft.commands.altitude :refer [apply-altitude]]
   [atc.engine.aircraft.commands.direct :refer [apply-direct]]
   [atc.engine.aircraft.commands.helpers :refer [normalize-heading utter-once]]
   [atc.engine.aircraft.commands.steering :refer [apply-steering]]
   [atc.engine.model :refer [angle-down-to bearing-to bearing-to->vec
                             distance-to-squared normalize v* vmag]]))


; ======= Report field in sight ===========================

(defn can-see-airport? [craft airport-position {:keys [visibility-sm]}]
  (or (nil? visibility-sm)
      (let [visibility-m (sm->m visibility-sm)
            distance-sq (distance-to-squared (:position craft) airport-position)]
        (<= distance-sq (* visibility-m visibility-m)))))

(defn apply-report-field-in-sight [craft {:keys [airport-position weather]} _dt]
  (cond-> craft
    (can-see-airport? craft airport-position weather)
    (-> (update :commands dissoc :report-field-in-sight)
        (utter-once "field in sight"))))


; ======= Visual Approaches ===============================

(defn compute-approach-leg [_aircraft {:keys [airport runway]}]
  (let [_runway-heading (airports/runway->heading airport runway)]
    ; TODO
    :enter-downwind))

(defmulti ^:private apply-visual-approach-leg
  (fn [aircraft _cmd _dt]
    (get-in aircraft [:behavior :visual-approach-state])))


; ======= Downwind leg ====================================

(defmethod apply-visual-approach-leg :downwind
  [aircraft {:keys [airport runway]} dt]
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
          (update :behavior assoc :visual-approach-state :downwind))

      (-> aircraft
          (apply-altitude target-altitude dt)
          (apply-steering target-heading dt)))))


; ======= Base leg ========================================

(def ^:private turn-final-distance-sq-m (* 500 500))

(defmethod apply-visual-approach-leg :base
  [aircraft {:keys [airport runway]} dt]
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
          (apply-direct final-turn-position dt)))))


; ======= Final approach ==================================

(def ^:private landed-distance-m 5)
(def ^:private min-glide-slope-degrees 1.6)

(defmethod apply-visual-approach-leg :final
  [aircraft {:keys [airport runway]} dt]
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

        ; TODO Slow down

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

(defn apply-visual-approach [aircraft cmd dt]
  (let [leg (or (get-in aircraft [:behavior :visual-approach-state])
                (compute-approach-leg aircraft cmd))]
    (-> aircraft
        (assoc-in [:behavior :visual-approach-state] leg)
        (apply-visual-approach-leg cmd dt))))

