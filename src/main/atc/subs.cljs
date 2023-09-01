(ns atc.subs
  (:require
   [atc.config :as config]
   [atc.data.airports :refer [runway->heading]]
   [atc.data.core :refer [local-xy]]
   [atc.engine.model :refer [v* vec3]]
   [atc.structures.rolling-history :refer [most-recent-n]]
   [atc.subs-util :refer [navaids-by-id]]
   [atc.util.subs :refer [get-or-identity]]
   [clojure.math :refer [floor]]
   [clojure.string :as str]
   [re-frame.core :refer [reg-sub]]))

(reg-sub
  :page
  :-> :page)


; ======= UI Config =======================================

(reg-sub
  ::ui-config
  :-> :ui-config)

(reg-sub
  :ui-config
  :<- [::ui-config]
  :=> get-or-identity)

(reg-sub
  :game-options
  :-> :game-options)

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
  :game/tracked-aircraft-map
  :<- [::engine]
  :-> :tracked-aircraft)

(reg-sub
  :game/aircraft
  :<- [:game/aircraft-map]
  :-> vals)

(reg-sub
  :game/tracked-aircraft
  :<- [:game/aircraft]
  :<- [:game/tracked-aircraft-map]
  (fn [[aircraft tracked-map]]
    (->> aircraft
         (filter #(get-in tracked-map [(:callsign %) :self?])))))

(reg-sub
  :game/aircraft-historical
  :<- [:game/tracked-aircraft]
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
  :-> navaids-by-id)

(reg-sub
  :game/airport-navaids
  :<- [:game/navaids-by-id]
  (fn [navaids]
    (when navaids
      (vals navaids))))

(reg-sub
  :game/airport-polygons
  :<- [:game/airport]
  (fn [{:keys [airspace-geometry] :as airport}]
    (->> airspace-geometry
         (map
           (fn [{:keys [id points]}]
             {:id id
              :points (map #(local-xy % airport) points)})))))

(reg-sub
  :game/neighboring-centers
  :<- [:game/airport]
  (fn [{:keys [center-facilities] :as airport}]
    (map-indexed
      (fn [idx {:keys [id frequency position]}]
        {:id id
         :label (str "SECTOR " (inc idx))
         :position (-> (local-xy position airport)
                       vec3
                       ; Push the positions away from the center of the map
                       ; to try to avoid overlap with navaids
                       (v* 1.8))
         :frequency frequency})
      center-facilities)))


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
  :game/weather
  :<- [:engine]
  :-> :weather)

(defn active-runways [airport weather]
  ; Use the weather, Luke
  (let [select-runway (:runway-selection airport)]
    (when (and weather select-runway)
      (select-runway weather))
    {:arrivals [(-> airport :runways first :start-id)]
     :departures [(-> airport :runways first :start-id)]}))

(reg-sub
  :game/active-runways
  :<- [:airport]
  :<- [:game/weather]
  :-> active-runways)

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
      1 :destination/aircraft-type
      2 :altitude/speed)))

(reg-sub
  :radio-history
  :-> :radio-history)


; ======= Game stats ======================================

(reg-sub
  :last-game
  :-> :last-game)

(reg-sub
  :last-game/summary
  :<- [:last-game]
  (fn [{:keys [engine game-events]}]
    (when game-events
      (let [events-by-type (->> game-events
                                (group-by :type))]
        {:airport (:airport engine)
         :elapsed-time (->> engine
                            :elapsed-s
                            floor) ; TODO format?
         :counts (->> events-by-type
                      (map (fn [[k events]]
                             [k (count events)]))
                      (into {}))}))))


; ======= UI ==============================================

(reg-sub
  :ui/range-rings
  :<- [:ui-config :range-rings-nm config/default-range-ring-nm]
  :-> (fn [range-nm]
        (range range-nm config/max-range-ring-nm range-nm)))
