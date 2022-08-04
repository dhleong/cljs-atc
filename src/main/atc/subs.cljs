(ns atc.subs
  (:require [re-frame.core :refer [reg-sub]]))

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
    (:state voice)))


; ======= Game state ======================================

(reg-sub ::engine :engine)

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
