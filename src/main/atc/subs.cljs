(ns atc.subs
  (:require
   [atc.data.core :refer [local-xy]]
   [re-frame.core :refer [reg-sub]]))

(reg-sub :page :page)

; ======= Voice ===========================================

(reg-sub ::voice :voice)

(reg-sub
  :voice/partial
  :<- [::voice]
  (fn [voice]
    (:partial-text voice)))

(reg-sub
  :voice/state
  :<- [::voice]
  (fn [voice]
    (if (:busy? voice)
      :busy
      (:state voice))))


; ======= Game state ======================================

(reg-sub ::engine :engine)

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
  :game/airport
  :<- [::engine]
  (fn [engine]
    (:airport engine)))

(reg-sub
  :game/airport-navaids
  :<- [:game/airport]
  (fn [airport]
    (when airport
      (->> airport
           :navaids
           (map (fn [{:keys [position] :as navaid}]
                  (merge navaid (local-xy position airport))))))))
