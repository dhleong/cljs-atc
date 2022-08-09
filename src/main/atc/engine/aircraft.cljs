(ns atc.engine.aircraft
  (:require
   [atc.engine.config :refer [AircraftConfig]]
   [atc.engine.model :refer [bearing-to ICommunicator Simulated v+ Vec3 vec3]]
   [atc.engine.pilot :as pilot]
   [clojure.math :refer [cos sin to-radians]]
   [clojure.string :as str]))

; ======= Instruction dispatch ============================

(defmulti dispatch-instruction (fn [_craft _context [instruction]] instruction))

(defmethod dispatch-instruction
  :steer
  [craft _ [_ heading steer-direction]]
  (let [heading-str (-> heading
                        (str)
                        (str/split #"")
                        (next)
                        (as-> numbers
                          ; TODO 9 -> niner, etc.
                          (str/join " " numbers)))]
    (-> craft
        ; TODO We need to a "human readable" string for viewing history (probably)
        ; and a speech-friendly string---for which we need the original
        ; airline/aircraft type and not the raw callsign
        (update ::utterance-parts conj (str (when steer-direction (name steer-direction))
                                            " "
                                            heading-str))

        (update :commands dissoc :direct)
        (update :commands assoc :heading heading :steer-direction steer-direction))))

(defmethod dispatch-instruction
  :direct
  [craft context [_ fix-id]]
  (if-let [fix (get-in context [:game/navaids-by-id fix-id])]
    (-> craft
        (update ::utterance-parts conj (str "direct " (:pronunciation fix)))
        (update :commands assoc :heading)
        (update :commands assoc :direct fix))

    ; TODO: Handle GA aircraft without the fix
    (-> craft
        (update ::utterance-parts conj "unable direct"))))

(defmethod dispatch-instruction
  :default
  [craft _context [instruction]]
  (println "TODO: Unhandled craft instruction: " instruction)
  craft)

; ======= Physics =========================================

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

(defn- apply-direct [^Aircraft a
                     commanded-to dt]
  ; NOTE: This is temporary; the real logic should account for resuming course,
  ; intercept heading, crossing altitude, etc.
  (let [bearing-to-destination (bearing-to (:position a) commanded-to)]
    (apply-steering a bearing-to-destination dt)))

(defn- apply-commanded-inputs [^Aircraft aircraft, commands dt]
  (cond-> aircraft
    (:heading commands) (apply-steering (:heading commands) dt)
    (:direct commands) (apply-direct (:direct commands) dt)))


; ======= Radiology =======================================

(defn build-utterance [craft parts]
  ; TODO We need to a "human readable" string for viewing history (probably)
  ; and a speech-friendly string---for which we need the original
  ; airline/aircraft type and not just the raw callsign
  (let [full-message (str
                       (str/join ", " parts)
                       ", "
                       (:callsign craft))]
    {:message full-message
     :from (assoc (:pilot craft)
                  :name (:callsign craft))}))


; ======= Main record =====================================

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defrecord Aircraft [^AircraftConfig config
                     ^String callsign pilot
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
      (update this :position v+ velocity-vector)))

  (command [this instruction]
    (dispatch-instruction this (:context (meta instruction)) instruction))

  ICommunicator
  (pending-communication [this]
    (build-utterance this (::utterance-parts this)))

  (prepare-pending-communication [this]
    (assoc this ::utterance-parts []))

  (consume-pending-communication [this]
    (dissoc this ::utterance-parts)))

(defn create [^AircraftConfig config, callsign]
  (map->Aircraft {:config config
                  :callsign callsign
                  :state :flight
                  :pilot (pilot/generate nil) ; TODO Pass in a preferred voice
                  :position (vec3 250 250 20000)
                  :heading 350
                  :speed 200
                  :commands {:heading 90}}))
