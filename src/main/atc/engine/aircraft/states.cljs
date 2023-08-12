(ns atc.engine.aircraft.states
  (:require
   [atc.config :as config]
   [atc.data.units :refer [m->ft]]
   [atc.engine.aircraft :as aircraft]
   [atc.util.numbers :refer [round-to-hundreds]]))

(defmulti update-state-machine
  "Update the engine *after* its aircrafts' physics have ticked.
   You may return `nil` to represent a no-op"
  (fn [engine callsign _dt]
    (get-in engine [:aircraft callsign :state])))

(defmethod update-state-machine :takeoff
  [engine callsign _dt]
  (let [aircraft (get-in engine [:aircraft callsign])
        track (get-in engine [:tracked-aircraft callsign])]
    (cond-> engine
      ; The aircraft isn't immediately "tracked" by anyone until it gets
      ; above the point where radar can track it (which we detect here)
      (and (nil? track)
           (>= (aircraft/altitude-agl-m
                 (:airport engine)
                 aircraft)
               config/min-radar-visbility-altitude-agl-m))
      (update :tracked-aircraft assoc callsign (get-in engine [:airport :positions :twr]))

      ; Once TWR observes altitude increase, they should handoff the
      ; aircraft to center
      (and (some? track)
           (not (:self? track))
           (>= (aircraft/altitude-agl-m
                 (:airport engine)
                 aircraft)
               config/min-twr->departure-handoff-altitude-agl-m))
      (->
        (update :tracked-aircraft assoc callsign {:self? true})
        (update-in [:aircraft callsign] assoc :state :flight)

        ; TODO simulate comms on TWR frequency here, first (or at least
        ; wait some time as though it were happening)
        (update :speech/enqueue conj
                {:from (aircraft/build-utterance-from aircraft)
                 ; TODO: include departure "name" (eg: "New York Departure")
                 :message ["Departure, " aircraft ". With you"
                           [:altitude
                            (round-to-hundreds
                              (m->ft (:z (:position aircraft))))]
                           (when-let [target-altitude (get-in aircraft
                                                              [:commands
                                                               :target-altitude])]
                             ["for" [:altitude (round-to-hundreds
                                                 target-altitude)]])]})))))

(defmethod update-state-machine :landed
  [engine callsign _dt]
  (let [aircraft (get-in engine [:aircraft callsign])]
    (-> engine
        (update :events conj {:type :aircraft-landed
                              :aircraft aircraft})
        (update :aircraft dissoc callsign))))

(defmethod update-state-machine :default
  [engine _callsign _dt]
  ; Default: nop
  engine)
