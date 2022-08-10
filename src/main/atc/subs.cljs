(ns atc.subs
  (:require
   [atc.data.core :refer [local-xy]]
   [atc.structures.rolling-history :refer [most-recent-n]]
   [clojure.string :as str]
   [re-frame.core :refer [reg-sub]]))

(reg-sub :page :page)

; ======= Voice ===========================================

(reg-sub ::voice :voice)

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


; ======= Game state ======================================

(reg-sub ::engine :engine)
(reg-sub ::game-history :game-history)

(reg-sub
  :game/recent-history
  :<- [::game-history]
  (fn [history]
    (reverse (most-recent-n 8 history))))

(reg-sub
  :game/time-scale
  :<- [::engine]
  (fn [engine]
    (:time-scale engine)))

(reg-sub
  :game/aircraft-map
  :<- [::engine]
  (fn [engine]
    (:aircraft engine)))

(reg-sub
  :game/aircraft
  :<- [:game/aircraft-map]
  (fn [aircraft]
    (vals aircraft)))

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
  (fn [engine]
    (:airport engine)))

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
