(ns atc.views.strips.view
  (:require
    [archetype.util :refer [<sub >evt]]
    [atc.views.strips.events :as events]
    [atc.views.strips.subs :as subs]
    [spade.core :refer [defattrs]]))

(defattrs flight-strips-attrs []
  {:background :*background-secondary*
   :color :*text*
   :height :100%})

(defn flight-strips []
  [:div (flight-strips-attrs)
   (when (= :popped-out (<sub [::subs/state]))
     [:button {:on-click #(>evt [::events/set-state :hidden])}
      "Pop in"])
   [:div.weather (str (<sub [:game/weather]))]])
