(ns atc.engine.aircraft.commands.visual-approach
  (:require
   [atc.data.units :refer [sm->m]]
   [atc.engine.aircraft.commands.helpers :refer [utter-once]]
   [atc.engine.model :refer [distance-to-squared]]))


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
