(ns atc.game
  (:require
   [archetype.util :refer [>evt]]
   [atc.data.airports :as airports]
   [atc.data.core :refer [local-xy]]
   [atc.engine.core :as engine]
   [promesa.core :as p]))

(defn init-async [{:keys [airport-id] :as opts}]
  {:pre [(keyword? airport-id)]}
  (println "(re) Initialize game engine @ " opts)
  (p/let [start (js/Date.now)
          airport (airports/load-airport airport-id)
          _ (println "Loaded " airport-id "in " (- (js/Date.now) start) "ms")
          new-engine (engine/generate airport)]
    (println "AC" (-> new-engine :aircraft vals first :position))
    (let [lga (-> new-engine :airport :navaids (nth 2))]
      (println (:id lga) (local-xy (-> lga :position)
                                   (-> new-engine :airport))))
    (>evt [:game/init-loaded-engine (assoc opts :engine new-engine)])))
