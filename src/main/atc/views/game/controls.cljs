(ns atc.views.game.controls
  (:require
   [archetype.util :refer [<sub >evt]]
   [spade.core :refer [defattrs]]))

(defattrs game-controls-attrs []
  {:display :flex
   :flex-direction :row})

(defn game-controls []
  [:div (game-controls-attrs)
    [:button {:on-click #(>evt [:game/init])}
     "Init Game"]
    [:button {:on-click #(>evt [:game/reset])}
     "End Game"]

    (when-let [time-scale (<sub [:game/time-scale])]
      (if (= 0 time-scale)
        [:button {:on-click #(>evt [:game/set-time-scale 1])}
         "Resume"]
        [:button {:on-click #(>evt [:game/set-time-scale 0])}
         "Pause"]))])
