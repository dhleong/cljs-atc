(ns atc.engine.aircraft.commands.altitude
  (:require
   [atc.engine.aircraft.commands.helpers :refer [apply-rate]]))

(defn apply-altitude [{{from :z} :position :as aircraft} commanded-altitude dt]
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
