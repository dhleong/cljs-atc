(ns atc.engine.core
  (:require
   [archetype.util :refer [>evt]]
   [atc.config :as config]
   [atc.data.aircraft-configs :as configs]
   [atc.data.airports :refer [runway->heading runway-coords]]
   [atc.data.units :refer [ft->m]]
   [atc.engine.aircraft :as aircraft]
   [atc.engine.model :as engine-model :refer [consume-pending-communication
                                              IGameEngine pending-communication
                                              prepare-pending-communication Simulated spawn-aircraft tick]]
   [atc.radio :as radio]
   [atc.util.maps :refer [rename-key]]
   [atc.voice.parsing.airport :as airport-parsing]
   [atc.voice.process :refer [build-machine]]))

(defn- dispatch-instructions [^Simulated simulated, context instructions]
  (when simulated
    (loop [simulated (prepare-pending-communication simulated)
           instructions (vec instructions)]
      (if (seq instructions)
        (recur (engine-model/command simulated (with-meta
                                                 (first instructions)
                                                 {:context context}))
               (next instructions))

        (do
          ; Done! Dispatch any pending communication
          (when-let [utterance (pending-communication simulated)]
            (>evt [:speech/enqueue utterance]))
          (consume-pending-communication simulated))))))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defrecord Engine [airport aircraft parsing-machine
                   tracked-aircraft
                   time-scale elapsed-s last-tick
                   events]
  ; NOTE: It is sort of laziness that we're implementing Simulated here,
  ; since we aren't, properly. Technically we should have a separate Simulator
  ; protocol and implement that to be more correct...
  Simulated
  (tick [this _dt]
    (let [now (js/Date.now)

          dt (* time-scale
                (if (some? last-tick)
                  (- now last-tick)
                  0)
                0.001)

          ; Tick all aircraft, and detect events like landing, etc.
          updated-aircraft (reduce-kv
                             (fn [m callsign aircraft]
                               (let [updated (tick aircraft dt)]
                                 (if (= :landed (:state updated))
                                   (update m ::events conj {:type :aircraft-landed
                                                            :aircraft updated})
                                   (assoc m callsign updated))))
                             {}
                             aircraft)]

      (assoc this
             :aircraft (dissoc updated-aircraft ::events)
             :elapsed-s (+ (:elapsed-s this) dt)
             :events (::events updated-aircraft)
             :last-tick (when-not (= 0 time-scale)
                          now))))

  (command [this command]
    ; NOTE: The Engine actually receives a full Command object so we know where
    ; to dispatch it and to whom.
    (let [{:keys [callsign instructions]} command]
      (if (some? (get-in this [:aircraft callsign]))
        (update-in this [:aircraft callsign] dispatch-instructions this instructions)

        ; There is no ~~spoon~~such aircraft:
        (do
          (println "WARNING: No such aircraft: " callsign)
          this))))

  IGameEngine
  (spawn-aircraft [this {:keys [config runway] :as opts}]
    (when (= :ga (:type opts))
      (throw (ex-info "GA aircraft not yet supported" {:opts opts})))

    ; TODO: Support arrivals
    (let [{:keys [callsign] :as radio} (radio/format-airline-radio
                                         (:airline opts)
                                         (:flight-number opts))
          data (merge
                 radio
                 (select-keys opts [:destination :config])
                 (-> this :airport :departure-routes
                     (get (:destination opts))
                     (rename-key :fix :departure-fix))
                 {:tx-frequency :self} ; NOTE: default to "my" frequency
                 (when runway
                   ; FIXME: This heading doesn't seem to *look* quite correct
                   ; TODO get target altitude from the airport/departure?
                   (let [position (first (runway-coords (:airport this) runway))]
                     {:heading (runway->heading (:airport this) runway)
                      :position position
                      :speed 0
                      :state :takeoff
                      :tx-frequency (get-in (:airport this)
                                            [:positions :twr :frequency])
                      :commands {:target-altitude (+ (ft->m 5000)
                                                     (:z position))
                                 :target-speed (min config/speed-limit-under-10k-kts
                                                    (:cruise-speed config))}})))]
      (update this :aircraft assoc callsign (aircraft/create config data)))))

(defn engine-grammar [^Engine engine]
  (get-in engine [:parsing-machine :fsm :grammar]))

(defn next-tick-delay [^Engine engine]
  (when-not (= 0 (:time-scale engine))
    (* 250 (/ 1 (:time-scale engine)))))

(defn generate [airport]
  ; TODO: Probably, generate the parsing-machine elsewhere for better loading states
  (let [aircraft [{:type :airline
                   :airline "DAL"
                   :flight-number 22
                   :destination (-> airport :departure-routes ffirst)
                   :runway (-> airport :runways first :start-id)
                   :config configs/common-jet}]]
    (reduce
      spawn-aircraft
      (map->Engine
        {:aircraft {}
         :tracked-aircraft {"DAL22" {:self? true}} ; TODO Empty this out when we handle handoffs
         :airport airport
         :parsing-machine (build-machine (airport-parsing/generate-parsing-context airport))
         :elapsed-s 0
         :events nil
         :time-scale 1})
      aircraft)))
