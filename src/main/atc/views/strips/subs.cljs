(ns atc.views.strips.subs
  (:require
   [re-frame.core :refer [reg-sub]]))

(reg-sub
  ::window
  :-> :flight-strip-window)
