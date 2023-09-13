(ns atc.engine.aircraft
  (:require
   [atc.data.units :refer [ft->m]]
   [atc.engine.aircraft.commands :refer [apply-commanded-inputs]]
   [atc.engine.aircraft.commands.helpers :refer [build-utterance-from]]
   [atc.engine.aircraft.instructions :as instructions :refer [dispatch-instruction]]
   [atc.engine.config :refer [AircraftConfig]]
   [atc.engine.model :refer [bearing-to ICommunicator Simulated v+ Vec3 vec3]]
   [atc.engine.pilot :as pilot]
   [clojure.math :refer [cos floor sin to-radians]]))

; ======= Physics =========================================

(defn- speed->mps [speed]
  (* 0.514444 speed))

(defn- heading->radians [heading]
  ; NOTE: We normalize the angle here such that 0 is "north" on the screen
  (to-radians (- heading 90)))

(defn altitude-agl-m [airport ^Aircraft aircraft]
  (- (:z (:position aircraft))
     (ft->m (last (:position airport)))))

; ======= Helpers =========================================

(defn departing-bearing [engine craft]
  (let [id->fix (:game/navaids-by-id engine)
        last-fix (->> craft
                      :departure-fix
                      id->fix)]
    (bearing-to (:position (:airport engine))
                last-fix)))

(defn choose-cruise-altitude-fl [engine craft]
  (let [default-fl (:cruise-flight-level (:config craft))
        default-fl-10th (floor (/ default-fl 10))
        alt-even? (= 0 (mod default-fl-10th 2))
        bearing (departing-bearing engine craft)]
    (cond
      ; NEODD
      (and (<= 0 bearing 179)
           alt-even?)
      (* 10 (- default-fl-10th 1))

      (<= 0 bearing 179)
      default-fl

      ; SWEVEN
      alt-even?
      default-fl

      :else
      (* 10 (- default-fl-10th 1)))))


; ======= Radiology =======================================

(defn- build-utterance [craft parts]
  (when (seq parts)
    {:message (conj parts ["," craft])
     :from (build-utterance-from craft)}))


; ======= Main record =====================================

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defrecord Aircraft [^AircraftConfig config
                     ^String callsign, ^String radio-name, pilot
                     tx-frequency
                     state
                     altitude-assignments
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
    (build-utterance this (::instructions/utterance-parts this)))

  (prepare-pending-communication [this]
    (assoc this ::instructions/utterance-parts []))

  (consume-pending-communication [this]
    (dissoc this ::instructions/utterance-parts)))

(defn create [^AircraftConfig config, {:keys [callsign radio-name] :as data}]
  (map->Aircraft (merge
                   {:config config
                    :callsign callsign
                    :radio-name radio-name
                    :state :flight
                    :pilot (pilot/generate nil) ; TODO Pass in a preferred voice?
                    :position (vec3 250 250 (ft->m 20000))
                    :altitude-assignments []
                    :heading 350
                    :speed 200}
                   data)))
