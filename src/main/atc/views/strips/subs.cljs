(ns atc.views.strips.subs
  (:require
   [re-frame.core :refer [reg-sub]]))

(reg-sub
  ::context
  :-> :flight-strips)

(reg-sub
  ::state
  :<- [::context]
  :-> :state)
