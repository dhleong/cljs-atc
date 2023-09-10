(ns atc.views.strips.events
  (:require
   [re-frame.core :refer [reg-event-fx trim-v]]))

(reg-event-fx
  ::set-state
  [trim-v]
  (fn [{:keys [db]} [state]]
    (when (= :popped-out state)
      (println "POP OUT")
      {:db (assoc-in db [:flight-strip-window] (js/window.open "" "flight-strips" "popup"))})))

(comment
  (re-frame.core/dispatch [::set-state :popped-out]))
