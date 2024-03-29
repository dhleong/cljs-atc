(ns atc.engine.core
  (:require
   [archetype.util :refer [>evt]]
   [atc.config :as config]
   [atc.data.airports :refer [runway->heading runway-coords]]
   [atc.data.units :refer [ft->m m->ft]]
   [atc.engine.aircraft :as aircraft :refer [choose-cruise-altitude-fl]]
   [atc.engine.aircraft.states :refer [update-state-machine]]
   [atc.engine.global :refer [dispatch-global-instruction]]
   [atc.engine.model :as engine-model :refer [consume-pending-communication
                                              IGameEngine pending-communication
                                              prepare-pending-communication Simulated tick]]
   [atc.engine.queues :refer [run-queues]]
   [atc.game.traffic.shared-util :refer [partial-arrival-route]]
   [atc.radio :as radio]
   [atc.util.maps :refer [rename-key]]
   [atc.voice.process :refer [build-machine]]))

(defn- dispatch-instructions [^Simulated simulated, context instructions]
  (when simulated
    (loop [simulated (prepare-pending-communication simulated)
           instructions (vec instructions)]
      (if (seq instructions)
        (recur (engine-model/command simulated context (first instructions))
               (next instructions))

        (do
          ; Done! Dispatch any pending communication
          (when-let [utterance (pending-communication simulated)]
            (>evt [:speech/enqueue utterance]))
          (consume-pending-communication simulated))))))

(defn- update-engine-states [engine callsigns dt]
  (let [engine' (reduce
                  (fn [engine' callsign]
                    (or (update-state-machine engine' callsign dt)
                        engine'))
                  (assoc engine :speech/enqueue [])
                  callsigns)]
    (doseq [enqueued (:speech/enqueue engine')]
      (>evt [:speech/enqueue enqueued]))
    (dissoc engine' :speech/enqueue)))

(defn- departing-aircraft-params [this {:keys [config runway] :as craft}]
  ; FIXME: This heading doesn't seem to *look* quite correct
  ; TODO get target altitude from the airport/departure?
  (let [position (first (runway-coords (:airport this) runway))
        cruise-flight-level (choose-cruise-altitude-fl this craft)]
    {:heading (runway->heading (:airport this) runway)
     :position position
     :speed 0
     :state :takeoff
     :cruise-flight-level cruise-flight-level
     :tx-frequency (get-in (:airport this)
                           [:positions :twr :frequency])
     :commands {:target-altitude (+ (ft->m 5000)
                                    (:z position))
                :target-speed (min config/speed-limit-under-10k-kts
                                   (:cruise-speed config))}}))

(defn- arriving-aircraft-params [this {:keys [config position heading] :as craft}]
  {:heading heading
   :position position
   :speed (:cruise-speed config)
   :state :arriving
   :arrival-fix (->> (partial-arrival-route this craft)
                     last)
   :altitude-assignments [{:direction :descend
                           :altitude-ft (-> (:z position)
                                            (m->ft))}]
   :tx-frequency (->> (:airport this)
                      :center-facilities
                      ; TODO Pick the actual closest center facility
                      first
                      :frequency)
   ; TODO follow arrival route
   :commands {}})

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defrecord Engine [airport aircraft parsing-machine
                   tracked-aircraft
                   traffic queues
                   time-scale elapsed-s last-tick
                   events]
  ; NOTE: It is sort of laziness that we're implementing Simulated here,
  ; since we aren't, properly. Technically we should have a separate Simulator
  ; protocol and implement that to be more correct...
  Simulated
  (tick [this _context _dt]
    (let [now (js/Date.now)

          dt (* time-scale
                (if (some? last-tick)
                  (- now last-tick)
                  0)
                0.001)

          ; Tick all aircraft
          updated-aircraft (reduce
                             (fn [m callsign]
                               (update m callsign tick this dt))
                             aircraft
                             (keys aircraft))

          ; Stash back in the engine, with updated timestamps
          engine (assoc this
                        :aircraft updated-aircraft
                        :elapsed-s (+ (:elapsed-s this) dt)
                        :last-tick (when-not (= 0 time-scale)
                                     now))]

      (-> engine
          ; Now update the engine, detecting any events like landing, etc.
          (update-engine-states (keys aircraft) dt)

          ; And process any "queued" events (like generating departures)
          (run-queues))))

  (command [this command _instuction]
    ; NOTE: The Engine receives a full Command object as its context, so we
    ; know where to dispatch it and to whom.
    (let [{:keys [callsign global? instructions]} command]
      (cond
        global?
        (reduce
          dispatch-global-instruction
          this
          instructions)

        (some? (get-in this [:aircraft callsign]))
        (update-in this [:aircraft callsign] dispatch-instructions this instructions)

        ; There is no ~~spoon~~such aircraft:
        :else
        (do
          (println "WARNING: No such aircraft: " callsign)
          ; This is a bit impure, but we're doing it for many other things...
          (>evt [:help/warning (str "No such aircraft: " callsign)])
          this))))

  IGameEngine
  (spawn-aircraft [this {:keys [config runway] :as opts}]
    (when (= :ga (:type opts))
      (throw (ex-info "GA aircraft not yet supported" {:opts opts})))

    (let [{:keys [callsign] :as radio} (radio/format-airline-radio
                                         (:airline opts)
                                         (:flight-number opts))
          arrival? (nil? runway)
          data (merge
                 radio
                 (select-keys opts [:destination :config])
                 (-> this :airport :departure-routes
                     (get (:destination opts))
                     (rename-key :fix :departure-fix))
                 {:tx-frequency :self} ; NOTE: default to "my" frequency
                 (select-keys opts [:origin :pilot :route :squawk])
                 (if arrival?
                   (arriving-aircraft-params this opts)
                   (departing-aircraft-params this opts)))]
      (println "[engine] spawn" (if arrival? :arrival :departure) callsign)
      (cond-> this
        true (update :aircraft assoc callsign (aircraft/create config data))
        arrival? (assoc-in [:tracked-aircraft callsign]
                           {:track-symbol "C"
                            :frequency (:tx-frequency data)})))))

(defn engine-grammar [^Engine engine]
  (get-in engine [:parsing-machine :fsm :grammar]))

(defn next-tick-delay [^Engine engine]
  (when-not (= 0 (:time-scale engine))
    (* 250 (/ 1 (:time-scale engine)))))

(defn generate [{:keys [airport traffic]}]
  (map->Engine
    {:aircraft {}
     :tracked-aircraft {}
     :airport airport
     :traffic traffic
     :parsing-machine (build-machine airport)
     :elapsed-s 0
     :events []
     :queues {}
     :time-scale 1}))
