(ns atc.engine.aircraft
  (:require
   [atc.engine.aircraft.commands :refer [apply-commanded-inputs]]
   [atc.engine.aircraft.instructions :refer [dispatch-instruction]]
   [atc.engine.config :refer [AircraftConfig]]
   [atc.engine.model :refer [ICommunicator Simulated v+ Vec3 vec3]]
   [atc.engine.pilot :as pilot]
   [clojure.math :refer [cos sin to-radians]]))

; ======= Physics =========================================

(defn- speed->mps [speed]
  (* 0.514444 speed))

(defn- heading->radians [heading]
  ; NOTE: We normalize the angle here such that 0 is "north" on the screen
  (to-radians (- heading 90)))

; ======= Radiology =======================================

(defn- build-utterance [craft parts]
  (when (seq parts)
    {:message (conj parts ["," craft])
     :from (assoc (:pilot craft)
                  :name (:callsign craft))}))


; ======= Main record =====================================

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defrecord Aircraft [^AircraftConfig config
                     ^String callsign, ^String radio-name, pilot
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

(defn create [^AircraftConfig config, {:keys [callsign radio-name]}]
  (map->Aircraft {:config config
                  :callsign callsign
                  :radio-name radio-name
                  :state :flight
                  :pilot (pilot/generate nil) ; TODO Pass in a preferred voice?
                  :position (vec3 250 250 20000)
                  :heading 350
                  :speed 200
                  :commands {:heading 90}}))
