(ns atc.engine.aircraft.commands.speed
  (:require
   [atc.engine.aircraft.commands.helpers :refer [apply-rate]]))

(defn apply-target-speed [{from :speed :as aircraft} commanded-speed dt]
  (if (= from commanded-speed)
    aircraft

    (apply-rate
      aircraft
      :target-speed [:speed]
      {-1 :deceleration
       1 :acceleration}
      from commanded-speed dt)))
