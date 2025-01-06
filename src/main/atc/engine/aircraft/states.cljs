(ns atc.engine.aircraft.states
  (:require
   [atc.config :as config]
   [atc.data.units :refer [m->ft]]
   [atc.engine.aircraft :as aircraft]
   [atc.engine.aircraft.commands.helpers :refer [message-from]]
   [atc.engine.model :refer [lateral-distance-to-squared]]
   [atc.util.numbers :refer [round-to-hundreds]]))

(defmulti update-state-machine
  "Update the engine *after* its aircrafts' physics have ticked.
   You may return `nil` to represent a no-op"
  (fn [engine callsign _dt]
    (get-in engine [:aircraft callsign :state])))

(defmethod update-state-machine :handing-off
  [engine callsign _dt]
  (let [aircraft (get-in engine [:aircraft callsign])
        handoff-data (get-in aircraft [:commands :handoff-to])]
    (cond-> engine
      true (assoc-in [:aircraft callsign :state] (case (:position handoff-data)
                                                   :center :flight
                                                   :tower :landing
                                                   :ground :taxi))
      true (update :tracked-aircraft assoc callsign (:track handoff-data))

      ; If we just handed off to center, this plane is out of here!
      ; TODO Center might reject the handoff if they're not near the edge
      ; of our airspace....
      (= :center (:position handoff-data))
      (update :events conj {:type :aircraft-departed
                            :aircraft aircraft}))))

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
        (update-in [:aircraft callsign] assoc :state :handed-off-to-self)))))

(defmethod update-state-machine :handed-off-to-self
  [engine callsign _dt]
  (let [aircraft (get-in engine [:aircraft callsign])
        named-position (if (= (:destination aircraft)
                              (:id (:airport engine)))
                         "Approach"
                         "Departure")]
    (->
      engine
      (update :tracked-aircraft assoc callsign {:self? true})
      (update-in [:aircraft callsign] assoc :state :flight)

      ; TODO simulate comms on frequency here, first (or at least
      ; wait some time as though it were happening)
     (update :speech/enqueue conj
             (message-from
              aircraft
               ; TODO: include approach/departure "name"
               ; (eg: "New York Departure")
              [named-position ", " aircraft ". "
               (cond
                 (get-in aircraft [:behavior :will-get-weather?])
                 "With the weather,"

                 :else "With you")
               [:altitude
                (round-to-hundreds
                 (m->ft (:z (:position aircraft))))]
               (when-let [target-altitude (get-in aircraft
                                                  [:commands
                                                   :target-altitude])]
                 ["for" [:altitude (round-to-hundreds
                                    target-altitude)]])])))))

(defmethod update-state-machine :arriving
  [engine callsign _dt]
  (let [aircraft (get-in engine [:aircraft callsign])
        distance-sq (lateral-distance-to-squared
                      {:x 0 :y 0}
                      (:position aircraft))]
    (cond-> engine
      (<= distance-sq config/ctr-control-radius-m-sq)
      (update-in [:aircraft callsign] assoc :state :handed-off-to-self))))

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
