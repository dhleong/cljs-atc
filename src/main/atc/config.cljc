(ns atc.config
  (:require
   [atc.data.units :refer [nm->m]]))

#?(:cljs (goog-define server-root ""))

(def default-game-options
  {:airport-id :kjfk
   :arrivals? true
   :departures? true
   :voice-input? true})

; Aircraft may not exceed 250kts below 10K ft
(def speed-limit-under-10k-kts 250)

(def min-radar-visbility-altitude-agl-m 10)
(def min-twr->departure-handoff-altitude-agl-m 25)

; *Rough* Radius under the CTR position's control in squared meters
(def ^:private ctr-control-radius-nm 35)
(def ctr-control-radius-m (nm->m ctr-control-radius-nm))
(def ctr-control-radius-m-sq (* ctr-control-radius-m
                                ctr-control-radius-m))

(def arrivals-lateral-spacing-m (nm->m 20))

; UI Config
(def default-range-ring-nm 5)
(def max-range-ring-nm 140)

; Game Config
(def initial-arrivals-to-spawn 10)

; If two navaids are within this distance, they are "overlapping"
(def navaid-overlap-m (nm->m 5))
