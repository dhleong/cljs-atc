(ns atc.data.units)

(defn m->nm [meters]
  (* 0.000539957 meters))

(defn nm->m [nautical-miles]
  (* 1852 nautical-miles))
