(ns atc.engine.core
  (:require
   [atc.data.aircraft-configs :as configs]
   [atc.engine.aircraft :as aircraft]
   [atc.engine.model :as engine-model :refer [Simulated tick]]
   [atc.voice.process :refer [build-machine]]
   [atc.voice.parsing.airport :as airport-parsing]))

(defn- dispatch-instructions [^Simulated simulated, context instructions]
  (when simulated
    (if (seq instructions)
      (recur (engine-model/command simulated (with-meta
                                               (first instructions)
                                               {:context context}))
             context
             (next instructions))
      simulated)))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defrecord Engine [airport aircraft parsing-machine time-scale last-tick]
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

          updated-aircraft (reduce-kv
                             (fn [m callsign aircraft]
                               (assoc m callsign (tick aircraft dt)))
                             {}
                             aircraft)]

      (assoc this
             :aircraft updated-aircraft
             :last-tick (when-not (= 0 time-scale)
                          now))))

  (command [this command]
    ; NOTE: The Engine actually receives a full Command object so we know where
    ; to dispatch it and to whom.
    (let [{:keys [callsign instructions]} command]
      (if (some? (get-in this [:aircraft callsign]))
        (update-in this [:aircraft callsign] dispatch-instructions this instructions)

        ; There is no ~~spoon~~such aircraft:
        this))))

(defn engine-grammar [^Engine engine]
  (get-in engine [:parsing-machine :fsm :grammar]))

(defn next-tick-delay [^Engine engine]
  (when-not (= 0 (:time-scale engine))
    (* 250 (/ 1 (:time-scale engine)))))

(defn- index-airport [airport]
  (assoc airport
         :navaids-by-id (reduce
                          (fn [m navaid]
                            (assoc m (:id navaid) navaid))
                          {}
                          (:navaids airport))))

(defn generate [airport]
  ; TODO: Probably, generate the parsing-machine elsewhere for better loading states
  (let [aircraft [["DAL22" configs/common-jet]]]
    (-> {:aircraft (reduce
                     (fn [m [callsign config]]
                       (assoc m callsign (aircraft/create config callsign)))
                     {}
                     aircraft)
         :airport (index-airport airport)
         :parsing-machine (build-machine (airport-parsing/generate-parsing-context airport))
         :time-scale 1}
        (map->Engine))))
