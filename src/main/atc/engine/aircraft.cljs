(ns atc.engine.aircraft
  (:require
    [clojure.math :refer [cos sin to-radians]]
    [atc.engine.config :refer [AircraftConfig]]
    [atc.engine.model :refer [->Vec3 Simulated v+ Vec3 vec3]]))

(defn- speed->mps [speed]
  (* 0.514444 speed))

(defn- normalize-heading [h]
  (if (< h 0)
    (+ h 360)
    (mod h 360)))

(defn- heading->radians [heading]
  ; NOTE: We normalize the angle here such that 0 is "north" on the screen
  (to-radians (- heading 90)))

(defn shorter-steer-direction [from to]
  ; With thanks to: https://math.stackexchange.com/a/2898118
  (let [delta (- (mod (- to from -540)
                      360)
                 180)]
    (if (>= delta 0)
      :right
      :left)))

(defn- apply-steering [^Aircraft {from :heading commands :commands :as aircraft}
                       commanded-to dt]
  (if (= from commanded-to)
    aircraft

    ; TODO at slower speeds, small craft might use a turn 2 rate (IE: 2x turn rate)
    ; TODO similarly, at higher speeds, large craft might use a half turn rate
    (let [turn-sign (case (or (:steer-direction commands)
                              (shorter-steer-direction from commanded-to))
                      :right  1
                      :left  -1)
          degrees-per-second (get-in aircraft [:config :turn-rate])
          turn-amount (* degrees-per-second dt)
          new-heading (normalize-heading (+ from (* turn-sign turn-amount)))]
      (if (<= (abs (- commanded-to new-heading))
              (* turn-amount 0.5))
        (-> aircraft
            (assoc :heading commanded-to)  ; close enough; snap to
            (dissoc :steer-direction))
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
          ; TODO: Vertical speed?
          raw-speed (* (speed->mps speed) dt)
          heading-radians (heading->radians (:heading this))

          vx (* raw-speed (cos heading-radians))
          vy (* raw-speed (sin heading-radians))
          velocity-vector (vec3 vx vy 0)]
      (update this :position v+ velocity-vector))))

(defn create [^AircraftConfig config, callsign]
  (map->Aircraft {:config config
                  :callsign callsign
                  :state :flight
                  :position (->Vec3 250 250 20000)
                  :heading 350
                  :speed 10
                  :commands {:heading 90}}))
