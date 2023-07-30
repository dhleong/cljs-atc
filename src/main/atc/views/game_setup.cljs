(ns atc.views.game-setup
  (:require
   [archetype.util :refer [>evt]]
   [garden.units :refer [px]]
   [promesa.core :as p]
   [reagent.core :as r]
   [spade.core :refer [defattrs]]))

(defattrs setup-container-attrs []
  {:align-items :center
   :background :*background*
   :color :*text*
   :display :flex
   :flex 1
   :justify-content :center
   :position :absolute
   :left 0
   :right 0
   :top 0
   :bottom 0}
  [:.title {:margin 0}]
  [:.content {:align-items :center
              :background :*background-secondary*
              :border-radius (px 8)
              :display :flex
              :flex-direction :column
              :padding (px 32)
              :width (px 400)}]
  [:.status {:visibility :hidden
             :padding (px 16)}
   [:&.loading {:visibility :visible}]])

(defn view []
  (r/with-let [loading? (r/atom false)]
    [:div (setup-container-attrs)
     [:div.content
      [:h1.title "ATC"]

      [:div.status {:class (when @loading? :loading)}
       "Reticulating splines..."]

      [:button {:disabled @loading?
                :on-click #(p/do
                             (reset! loading? true)
                             (p/delay 10)
                             (>evt [:game/init {:airport-id :kjfk}]))}
       "Start a new game"]]]))
