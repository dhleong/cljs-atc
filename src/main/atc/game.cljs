(ns atc.game
  (:require
   [archetype.util :refer [>evt]]
   [atc.data.airports :as airports]
   [atc.data.core :refer [local-xy]]
   [atc.engine.core :as engine]
   [atc.game.traffic.factory :refer [create-traffic]]
   [atc.util.spec :as spec]
   [clojure.spec.alpha :as s]
   [promesa.core :as p]
   [spec-tools.data-spec :as ds]))

(def ^:private game-options-defaults {:traffic :random})

(def ^:private traffic-spec
  (ds/or {:random (ds/or {:with-seed {(ds/opt :seed) number?}
                          :default-seed (s/spec #{:random})})
          :debug (s/spec #{:debug})}))

(def ^:private game-options-spec
  (ds/spec
    {:name ::game-options
     :spec {:airport-id keyword?
            (ds/opt :arrivals?) boolean?
            (ds/opt :departures?) boolean?
            (ds/opt :voice-input?) boolean?
            (ds/opt :traffic) traffic-spec}}))

(defn- expand-opts [opts]
  (s/conform game-options-spec
             (merge game-options-defaults opts)))

(defn init-async [{:keys [airport-id] :as opts}]
  {:pre [(spec/pre-validate game-options-spec opts)]}
  (-> (p/let [opts (expand-opts opts)
              _ (println "(re) Initialize game engine @ " opts)

              start (js/Date.now)
              airport (airports/load-airport airport-id)
              _ (println "Loaded " airport-id "in " (- (js/Date.now) start) "ms")
              traffic (create-traffic
                        (:traffic opts)
                        opts)

              new-engine (engine/generate {:airport airport
                                           :traffic traffic})]
        (println "AC" (-> new-engine :aircraft vals first :position))
        (let [lga (-> new-engine :airport :navaids (nth 2))]
          (println (:id lga) (local-xy (-> lga :position)
                                       (-> new-engine :airport))))
        (>evt [:game/init-loaded-engine (assoc opts :engine new-engine)]))

      (p/catch (fn [e]
                 ; TODO Notify UI somehow
                 (js/console.error "ERROR initializing game engine: " e)))))
