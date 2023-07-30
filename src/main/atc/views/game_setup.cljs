(ns atc.views.game-setup
  (:require
   [archetype.util :refer [>evt]]
   [garden.units :refer [px]]
   [promesa.core :as p]
   [reagent.core :as r]
   [santiago.form :refer [form]]
   [santiago.input :refer [input]]
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
   [:&.loading {:visibility :visible}]]

  [:.form {:display :flex
           :align-items :center
           :flex-direction :column
           :gap (px 8)}]
  [:.checkbox {:display :flex
               :font-size :105%
               :user-select :none
               :align-items :center}]
  [:.explanation {:text-align :center}]
  [:.spacer {:height (px 8)}])

(defn- start-game! [loading?-ref ^js e {:keys [use-voice-input]}]
  (.preventDefault e)
  (p/do
    (reset! loading?-ref true)
    (p/delay 10) ; Leave time to show loading state
    (>evt [:game/init {:airport-id :kjfk
                       :voice-input? use-voice-input}])))

(defn view []
  (r/with-let [form-value (r/atom {:use-voice-input true})
               loading? (r/atom false)
               on-start-game (partial start-game! loading?)]
    [:div (setup-container-attrs)
     [:div.content
      [:h1.title "cljs-atc"]

      [:div.status {:class (when @loading? :loading)}
       "Reticulating splines..."]

      [form {:class :form
             :model form-value
             :on-submit on-start-game}

       [:div.checkbox
        [input {:type :checkbox
                :aria-describedby ::voice-explanation
                :disabled @loading?
                :key :use-voice-input
                :id ::use-voice-input}]
        [:label {:for ::use-voice-input}
         "Use voice input"]]
       [:div.explanation {:id (str ::voice-explanation)}
        "If enabled, you will be prompted to allow microphone input once the game is loaded. You can then hold the spacebar to activate the mic and talk to pilots on your frequency!"]

       [:div.spacer]

       [:button {:disabled @loading?
                 :type :submit}
        "Start a new game"]]]]))
