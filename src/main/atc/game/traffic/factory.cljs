(ns atc.game.traffic.factory
  (:require
   [atc.game.traffic.model :refer [->ValidatedTraffic]]
   [atc.game.traffic.random :refer [->RandomTraffic]]
   [atc.util.seedable :refer [create-random]]
   [clojure.core.match :refer [match]]))

(defn create-traffic [spec]
  (cond->
    (match spec
      [:random [:default-seed _]]
      (->RandomTraffic
        (create-random))

      [:random [:with-seed {:seed seed}]]
      (->RandomTraffic
        (create-random seed)))

    ; Wrap with a validator in debug mode
    goog.DEBUG (->ValidatedTraffic)))

#_{:clj-kondo/ignore [:unresolved-namespace]}
(comment

  (atc.game.traffic.model/generate-initial-arrivals
    (create-traffic [:random [:default-seed nil]])
    {:airport atc.data.airports.kjfk/airport
     :game/navaids-by-id (atc.subs/navaids-by-id
                           atc.data.airports.kjfk/airport)}))
