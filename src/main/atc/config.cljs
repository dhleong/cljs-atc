(ns atc.config)

(goog-define server-root "")

; Aircraft may not exceed 250kts below 10K ft
(def speed-limit-under-10k-kts 250)

(def min-radar-visbility-altitude-agl-m 10)
(def min-twr->departure-handoff-altitude-agl-m 25)
