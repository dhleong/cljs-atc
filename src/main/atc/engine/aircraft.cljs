(ns atc.engine.aircraft
  (:require
   [atc.engine.config :refer [AircraftConfig]]
   [atc.engine.model :refer [->Vec3 Simulated v+ Vec3 vec3]]))

(defn- speed->mps [speed]
  (* 0.514444 speed))

(defn- apply-steering [^Aircraft {from :heading :as aircraft} commanded-to dt]
  (println "steer" aircraft)
  (if (= from commanded-to)
    aircraft

    ; TODO turn direction:
    (let [degrees-per-second (get-in aircraft [:config :turn-rate])
          turn-amount (* degrees-per-second (/ dt 1000))
          new-heading (+ from turn-amount)]
      (println commanded-to ":" from " -> " new-heading turn-amount)
      (if (<= (abs (- new-heading commanded-to))
              (* turn-amount 0.5))
        (assoc aircraft :heading commanded-to) ; close enough; snap to
        (assoc aircraft :heading new-heading)))))

(defn- apply-commanded-inputs [^Aircraft aircraft, commands dt]
  (cond-> aircraft
    (:heading commands) (apply-steering (:heading commands) dt)))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defrecord Aircraft [^AircraftConfig config
                     ^String callsign
                     state
                     ^Vec3 position heading speed
                     commands]
  Simulated
  (tick [this dt]
    (let [this (apply-commanded-inputs this commands dt)
          ; TODO: compute velocity vector properly
          velocity-vector (vec3 (* (/ dt 1000)
                                   (speed->mps speed))
                                0
                                0)]
      (update this :position v+ velocity-vector))))

(defn create [^AircraftConfig config, callsign]
  (map->Aircraft {:config config
                  :callsign callsign
                  :state :flight
                  :position (->Vec3 250 250 20000)
                  :heading 120
                  :speed 200
                  :commands {:heading 135}}))
