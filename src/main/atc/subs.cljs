(ns atc.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub :page :page)

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
