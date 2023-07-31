(ns atc.views.game-setup
  (:require
   [archetype.util :refer [>evt]]
   [atc.data.airports :refer [list-airports]]
   [garden.units :refer [px]]
   [promesa.core :as p]
   [reagent.core :as r]
   [santiago.form :refer [form]]
   [santiago.input :refer [input]]
   [santiago.select :refer [select]]
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
           :gap (px 8)}
   [:&.loading {:opacity 0.5}]]
  [:.labeled {:display :flex
              :font-size :105%
              :gap (px 4)
              :user-select :none
              :align-items :center}]
  [:.explanation {:text-align :center}]
  [:.spacer {:height (px 8)}])

(defn- start-game! [loading?-ref ^js e {:keys [airport-id use-voice-input]}]
  (.preventDefault e)
  (p/do
    (reset! loading?-ref true)
    (p/delay 10) ; Leave time to show loading state
    (>evt [:game/init {:airport-id airport-id
                       :voice-input? use-voice-input}])))

(defn view []
  (r/with-let [form-value (r/atom {:airport-id :kjfk
                                   :use-voice-input true})
               loading? (r/atom false)
               on-start-game (partial start-game! loading?)]
    [:div (setup-container-attrs)
     [:div.content
      [:h1.title "cljs-atc"]

      [:div.status {:class (when @loading? :loading)}
       "Reticulating splines..."]

      [form {:class [:form (when @loading? :loading)]
             :model form-value
             :on-submit on-start-game}

       [:div.labeled
        [:label {:for ::airport-id}
         "Airport"]
        [select {:id ::airport-id
                 :disabled @loading?
                 :key :airport-id}
         (for [{:keys [key label]} (list-airports)]
           ^{:key key}
           [:option {:key key} label])]]

       [:div.labeled
        [:label {:for ::use-voice-input}
         "Use voice input"]
        [input {:type :checkbox
                :aria-describedby ::voice-explanation
                :disabled @loading?
                :key :use-voice-input
                :id ::use-voice-input}]]
       [:div.explanation {:id ::voice-explanation}
        "If enabled, you will be prompted to allow microphone input once the game is loaded. You can then hold the spacebar to activate the mic and talk to pilots on your frequency!"]

       [:div.spacer]

       [:button {:disabled @loading?
                 :type :submit}
        "Start a new game"]]]]))
