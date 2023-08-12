(ns atc.views.game-setup
  (:require
   [archetype.util :refer [<sub >evt]]
   [atc.data.airports :refer [list-airports]]
   [atc.styles :refer [full-screen]]
   [garden.units :refer [px]]
   [promesa.core :as p]
   [reagent.core :as r]
   [santiago.form :refer [form]]
   [santiago.input :refer [input]]
   [santiago.select :refer [select]]
   [spade.core :refer [defattrs]]))

(defattrs last-game-info-attrs []
  {:margin-right (px 16)
   :background :*background-secondary*
   :border-radius (px 4)
   :padding (px 32)}
  [:.title {:margin 0}]
  [:.stats {:text-align :left
            :margin [[(px 16) 0]]}])

(defn- last-game-info []
  (when-let [{:keys [airport counts elapsed-time]} (<sub [:last-game/summary])]
    [:div (last-game-info-attrs)
     [:h3.title "Last game"]

     [:table.stats
      [:tr
       [:th "Airport"]
       [:td (:id airport)]]
      [:tr
       [:th "Elapsed Time"]
       [:td elapsed-time]]
      [:tr
       [:th "Aircraft Deparated"]
       [:td (:aircraft-departed counts 0)]]]

     [:button {:on-click #(>evt [:game/resume-last])}
      "Resume"]]))

(defattrs setup-container-attrs []
  {:composes (full-screen)
   :background :*background*
   :color :*text*
   :flex 1}
  [:.title {:margin 0}]
  [:.content {:align-items :center
              :background :*background-secondary*
              :border-radius (px 4)
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
     [last-game-info]

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
