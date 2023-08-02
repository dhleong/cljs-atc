(ns atc.views.pause-screen
  (:require
   [archetype.util :refer [>evt]]
   [atc.styles :refer [full-screen]]
   [atc.styles.css :refer [linear-gradient]]
   [garden.units :refer [px]]
   [spade.core :refer [defattrs]]))

(defattrs scrim-attrs []
  {:composes (full-screen)
   :background (linear-gradient
                 :90deg
                 [:*background-secondary* :0%]
                 ["rgba(0, 0, 0, 0.7)" :42%]
                 ["#00000000" :80%])
   :pointer-events :none})

(defattrs pause-screen-attrs []
  {:composes (full-screen {:position :fixed
                           :justify-content :flex-start})
   :color :*text*
   :pointer-events :none}
  [:.content {:display :flex
              :align-items :center
              :flex-direction :column
              :padding (px 16)
              :pointer-events :auto
              :user-select :none
              :width :30%}]
  [:.header {:margin 0}]
  [:.help {:padding (px 16)
           :text-align :center}]
  [:.spacer {:height (px 8)}])

(defn view []
  [:<>
   [:div (scrim-attrs)]
   [:div (pause-screen-attrs)
    [:div.content
     [:h2.header "Paused"]

     [:div.help "This is an ATC simulation. This place should have some links to help docs at some point."]

     [:button {:on-click #(>evt [:game/set-time-scale 1])}
      "Resume"]
     [:div.spacer]
     [:button.destructive {:on-click #(>evt [:game/reset])}
      "End Game"]]]])
