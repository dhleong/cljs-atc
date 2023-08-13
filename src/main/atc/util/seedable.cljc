(ns atc.util.seedable
  (:require
   [clojure.math :refer [floor]]
   [clojure.test.check.random :as random :refer [make-random]]))

(defprotocol IStatefulRandom
  (next-double [this])
  (next-int [this low high])
  (pick-random [this values]))

(defrecord StatefulRandom [state]
  IStatefulRandom
  (next-double [_]
    (random/rand-double
      (::returned
        (swap! state (fn [{:keys [state]}]
                       (let [[state' returned] (random/split state)]
                         {:state state'
                          ::returned returned}))))))

  (next-int [this low high]
    (let [v (next-double this)]
      (+ low (floor (* (- high low)
                       v)))))

  (pick-random [this values]
    (let [idx (next-int this 0 (count values))]
      (nth
        (if (vector? values)
          values
          (seq values))
        idx))))

(defn create-random
  ([] (create-random #?(:cljs (js/Date.now)
                        :clj (System/currentTimeMiillis))))
  ([seed] (->StatefulRandom (atom {:state (make-random seed)}))))
