(ns atc.engine.aircraft.commands.direct
  (:require
   [atc.engine.aircraft.commands.steering :refer [apply-steering]]
   [atc.engine.model :refer [bearing-to distance-to-squared]]
   [clojure.math :refer [pow]]))

; ======= Direct-to-point nav =============================

; This is the distance in meters squared at which an aircraft is considered
; to have "arrived" at its :direct target. We include quite a bit of slop
; to avoid excess steering, since you should maintain heading if you pass
; the point and don't have any other direction
(def ^:private over-coordinate-distance-m2 (pow 6500 2.))

(defn apply-direct [aircraft commanded-to dt]
  ; NOTE: This is temporary; the real logic should account for resuming course,
  ; intercept heading, crossing altitude, etc.
  (let [distance2 (distance-to-squared (:position aircraft) commanded-to)]
    (if (< distance2 over-coordinate-distance-m2)
      ; We're "at" the destination; we can stop heading towards it now
      (update aircraft :commands dissoc :direct)

      (let [bearing-to-destination (bearing-to (:position aircraft) commanded-to)]
        (apply-steering aircraft bearing-to-destination dt)))))
