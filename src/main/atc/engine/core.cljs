(ns atc.engine.core
  (:require
   [atc.engine.aircraft :as aircraft]
   [atc.engine.model :refer [Simulated tick]]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defrecord Engine [aircraft]
  Simulated
  (tick [this dt]
    (let [updated-aircraft (reduce-kv
                             (fn [m callsign aircraft]
                               (assoc m callsign (tick aircraft dt)))
                             {}
                             aircraft)]
      (assoc this :aircraft updated-aircraft))))

(defn generate []
  (-> {:aircraft {"DAL22" (aircraft/create {} "DAL22")}}
      (map->Engine)))
