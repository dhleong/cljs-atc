(ns atc.game.traffic.factory
  (:require
   [atc.game.traffic.filtered :refer [->FilteredTraffic]]
   [atc.game.traffic.model :refer [->ValidatedTraffic]]
   [atc.game.traffic.random :refer [->RandomTraffic]]
   [atc.util.seedable :refer [create-random]]
   [clojure.core.match :refer [match]]))

(defn create-traffic [spec {:keys [arrivals? departures?]}]
  (cond->
    (match spec
      [:random [:default-seed _]]
      (->RandomTraffic
        (create-random))

      [:random [:with-seed {:seed seed}]]
      (->RandomTraffic
        (create-random seed)))

    ; Wrap with a validator in debug mode
    goog.DEBUG (->ValidatedTraffic)

    (not (and arrivals?
              departures?)) (->FilteredTraffic arrivals? departures?)))
