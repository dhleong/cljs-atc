(ns atc.config
  (:require
   [atc.data.units :refer [nm->m]]))

(goog-define server-root "")

; Aircraft may not exceed 250kts below 10K ft
(def speed-limit-under-10k-kts 250)

(def min-radar-visbility-altitude-agl-m 10)
(def min-twr->departure-handoff-altitude-agl-m 25)

; *Rough* Radius under the CTR position's control in squared meters
(def ^:private ctr-control-radius-nm 25)
(def ctr-control-radius-m-sq (let [m (nm->m ctr-control-radius-nm)]
                               (* m m)))

; UI Config
(def default-range-ring-nm 5)
(def max-range-ring-nm 140)

; Game Config
(def initial-arrivals-to-spawn 10)
