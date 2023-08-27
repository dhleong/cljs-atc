(ns atc.game.traffic.filtered
  (:require
   [atc.game.traffic.model :refer [ITraffic next-arrival next-departure
                                   spawn-initial-arrivals]]))

(defrecord FilteredTraffic [base arrivals? departures?]
  ITraffic
  (spawn-initial-arrivals [_this engine]
    (if arrivals?
      (spawn-initial-arrivals base engine)
      {:engine engine
       :delay-to-next-s 240}))
  (next-arrival [_this engine]
    (cond->
      (next-arrival base engine)
      (not arrivals?) (dissoc :aircraft)))
  (next-departure [_this engine]
    (cond->
      (next-departure base engine)
      (not departures?) (dissoc :aircraft))))
