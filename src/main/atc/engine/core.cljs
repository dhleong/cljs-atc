(ns atc.engine.core
  (:require
   [atc.data.aircraft-configs :as configs]
   [atc.engine.aircraft :as aircraft]
   [atc.engine.model :refer [Simulated tick]]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defrecord Engine [aircraft time-scale last-tick]
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
                          now)))))

(defn next-tick-delay [^Engine engine]
  (when-not (= 0 (:time-scale engine))
    (* 250 (/ 1 (:time-scale engine)))))

(defn generate []
  (-> {:aircraft {"DAL22" (aircraft/create configs/common-jet "DAL22")}
       :time-scale 1}
      (map->Engine)))
