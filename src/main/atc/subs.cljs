(ns atc.subs
  (:require
   [atc.data.airports :refer [runway->heading]]
   [atc.data.core :refer [local-xy]]
   [atc.structures.rolling-history :refer [most-recent-n]]
   [clojure.string :as str]
   [re-frame.core :refer [reg-sub]]))

(reg-sub
  :page
  :-> :page)

; ======= Voice ===========================================

(reg-sub
  ::voice
  :-> :voice)

(reg-sub
  :voice/partial
  :<- [::voice]
  (fn [voice]
    (str/join " " (conj
                    (:pending-results voice)
                    (:partial-text voice)))))

(reg-sub
  :voice/state
  :<- [::voice]
  (fn [voice]
    (if (:busy? voice)
      :busy
      (:state voice))))

(reg-sub
  :voice/recording?
  :<- [::voice]
  :-> (complement :paused?))

(reg-sub
  :voice/requested?
  :<- [:engine-config]
  :-> :voice-input?)


; ======= Game state ======================================

(reg-sub
  ::engine
  :-> :engine)

(reg-sub
  :engine-config
  :-> :engine-config)

(reg-sub
  ::game-history
  :-> :game-history)

(reg-sub
  :game/started?
  :<- [::engine]
  :-> some?)

(reg-sub
  :game/recent-history
  :<- [::game-history]
  (fn [history]
    (reverse (most-recent-n 8 history))))

(reg-sub
  :game/time-scale
  :<- [::engine]
  :-> :time-scale)

(reg-sub
  :game/paused?
  :<- [:game/time-scale]
  :-> (partial = 0))

(reg-sub
  :game/aircraft-map
  :<- [::engine]
  :-> :aircraft)

(reg-sub
  :game/aircraft
  :<- [:game/aircraft-map]
  :-> vals)

(reg-sub
  :game/aircraft-historical
  :<- [:game/aircraft]
  :<- [:game/recent-history]
  (fn [[current history]]
    (->> current
         (mapcat (fn [{:keys [callsign]}]
                   (map-indexed
                     (fn [i history-entry]
                       (-> history-entry
                           (get-in [:aircraft callsign])
                           (assoc :history-n i)))
                     history))))))

(reg-sub
  :game/airport
  :<- [::engine]
  :-> :airport)

(reg-sub
  :game/navaids-by-id
  :<- [:game/airport]
  (fn [airport]
    (when airport
      (reduce
        (fn [m {:keys [position] :as navaid}]
          (assoc m (:id navaid)
                 (merge navaid (local-xy position airport))))
        {}
        (:navaids airport)))))

(reg-sub
  :game/airport-navaids
  :<- [:game/navaids-by-id]
  (fn [navaids]
    (when navaids
      (vals navaids))))

(reg-sub
  :game/airport-runway-ids
  :<- [:game/airport]
  (fn [airport]
    (when airport
      (->> airport
           :runways
           (mapcat (juxt :start-id :end-id))
           (into #{})))))

(reg-sub
  :game/runways
  :<- [:game/airport]
  (fn [airport]
    (->> airport
         :runways
         (map (fn [rwy]
                (-> rwy
                    (assoc :position {:x 0 :y 0})
                    (assoc :start-angle (runway->heading airport (:start-id rwy)))
                    (assoc :end-angle (runway->heading airport (:end-id rwy)))
                    (update :start-threshold local-xy airport)
                    (update :end-threshold local-xy airport)))))))

(reg-sub
  :ui/tick
  :-> :ui/tick)

(reg-sub
  :datablock-mode/full
  :<- [:ui/tick]
  (fn [tick]
    ; NOTE: to simplify timing (showing altitude/speed longer than exit fix)
    ; we include some duplicates here and keep a fixed tick duration.
    ; Eventually there might be other datablock modes to rotate between?
    (case (mod tick 3)
      0 :altitude/speed
      1 :exit-fix/aircraft-type
      2 :altitude/speed)))

(reg-sub
  :radio-history
  :-> :radio-history)
