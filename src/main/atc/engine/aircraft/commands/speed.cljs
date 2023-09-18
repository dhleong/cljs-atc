(ns atc.engine.aircraft.commands.speed
  (:require
   [atc.config :as config]
   [atc.data.units :refer [ft->m]]
   [atc.engine.aircraft.commands.helpers :refer [apply-rate]]))

(def ^:private speed-limit-altitude-m (ft->m 10000))

(defn apply-target-speed [{from :speed :as aircraft} raw-commanded-speed dt]
  (let [altitude (get-in aircraft [:position :z])
        commanded-speed (if (> altitude speed-limit-altitude-m)
                          raw-commanded-speed
                          (min config/speed-limit-under-10k-kts
                               raw-commanded-speed))]
    (if (= from commanded-speed)
      aircraft

      (apply-rate
        aircraft
        :target-speed [:speed]
        {-1 :deceleration
         1 :acceleration}
        from commanded-speed dt))))
