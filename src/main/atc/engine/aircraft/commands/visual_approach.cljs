(ns atc.engine.aircraft.commands.visual-approach
  (:require
   [atc.data.airports :as airports :refer [runway->heading runway-coords]]
   [atc.data.units :refer [ft->m sm->m]]
   [atc.engine.aircraft.commands.altitude :refer [apply-altitude]]
   [atc.engine.aircraft.commands.helpers :refer [normalize-heading utter-once]]
   [atc.engine.aircraft.commands.steering :refer [apply-steering]]
   [atc.engine.model :refer [bearing-to distance-to-squared]]))


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

(defn apply-visual-approach [aircraft cmd dt]
  (let [leg (or (get-in aircraft [:behavior :visual-approach-state])
                (compute-approach-leg aircraft cmd))]
    (-> aircraft
        (assoc-in [:behavior :visual-approach-state] leg)
        (apply-visual-approach-leg cmd dt))))

