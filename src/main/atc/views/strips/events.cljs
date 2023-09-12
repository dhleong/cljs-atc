(ns atc.views.strips.events
  (:require
   [re-frame.core :refer [path reg-event-db trim-v]]))

(reg-event-db
  ::set-state
  [trim-v (path :flight-strips)]
  (fn [flight-strips [state]]
    (assoc flight-strips :state state)))

(comment
  (re-frame.core/dispatch [::set-state :popped-out]))
