(ns atc.engine.aircraft.commands
  "Responding to commands"
  (:require
   [atc.engine.model :refer [bearing-to]]))

(defn- normalize-heading [h]
  (if (< h 0)
    (+ h 360)
    (mod h 360)))

(defn shorter-steer-direction [from to]
  ; With thanks to: https://math.stackexchange.com/a/2898118
  (let [delta (- (mod (- to from -540)
                      360)
                 180)]
    (if (>= delta 0)
      :right
      :left)))

(defn- apply-steering [{from :heading commands :commands :as aircraft}
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

(defn- apply-direct [aircraft commanded-to dt]
  ; NOTE: This is temporary; the real logic should account for resuming course,
  ; intercept heading, crossing altitude, etc.
  (let [bearing-to-destination (bearing-to (:position aircraft) commanded-to)]
    (apply-steering aircraft bearing-to-destination dt)))


; ======= Publc interface =================================

(defn apply-commanded-inputs [aircraft, commands dt]
  (cond-> aircraft
    (:heading commands) (apply-steering (:heading commands) dt)
    (:direct commands) (apply-direct (:direct commands) dt)))
