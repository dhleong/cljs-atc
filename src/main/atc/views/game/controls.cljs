(ns atc.views.game.controls
  (:require
   ["react" :as React]
   [archetype.util :refer [<sub >evt]]
   [atc.components.bottom-scroller :refer [bottom-scroller]]
   [atc.styles.css :refer [with-opacity]]
   [garden.units :refer [px]]
   [spade.core :refer [defattrs]]))

(defattrs radio-history-attrs []
  {:background (with-opacity :*background-secondary* 0.5)
   :color :*text*
   :display :flex
   :flex-direction :column
   :overflow-y :scroll
   :overscroll-behavior :contain
   :padding (px 8)
   :height (px 120)
   :width (px 400)}
  [:.list {:list-style :none
           :margin 0
           ; bottom-align items in the view:
           :margin-top :auto
           :padding 0}]
  [:.history-entry
   [:&.self {:font-style :italic
             :opacity 0.8
             :margin-top (px 2)}]])

(defn- radio-history []
  [bottom-scroller (radio-history-attrs)
   [:ul.list {:aria-label "Radio History"}
    (for [history (<sub [:radio-history])]
      ^{:key (:timestamp history)}
      [:li.history-entry {:class (when (:self? history)
                                   :self)}
       "[" (:speaker history) "] "
       (:text history)])]])

(defattrs voice-controls-active-attrs [recording?]
  [:.mic {:opacity (if recording? 0.8 1.0)}])

(defn- voice-controls-active [{:keys [show-disable?]}]
  (React/useEffect
    (fn []
      (>evt [:voice/enable-keypresses])
      #(>evt [:voice/disable-keypresses])))

  [:div (voice-controls-active-attrs (<sub [:voice/recording?]))
   (when show-disable?
     [:button {:on-click #(>evt [:voice/stop!])}
      "Disable Mic"])

   (when (= :ready (<sub [:voice/state]))
     [:<>
      [:button.mic {:on-mouse-down #(>evt [:voice/set-paused false])
                    :on-mouse-up #(>evt [:voice/set-paused true])}
       "MIC (hold this or space)"]
      (<sub [:voice/partial])])])

(defn- voice-controls []
  (let [voice-requested? (<sub [:voice/requested?])
        state (<sub [:voice/state])]
    (if voice-requested?
      (case state
        (nil :ready) [:f> voice-controls-active {:show-disable? false}]
        [:button {:disabled true}
         "(" (name state) ")"])

      ; Voice input not requested with setup; show controls to enable
      (case state
        nil [:button {:on-click #(>evt [:voice/start!])}
             "Enable Microphone"]
        :ready [:f> voice-controls-active]
        [:button {:disabled true}
         "(" (name state) ")"]))))

(defattrs game-controls-attrs []
  {:display :flex
   :flex-direction :column}
  [:.buttons {:display :flex
              :flex-direction :row}])

(defn game-controls []
  (let [time-scale (<sub [:game/time-scale])]
    [:div (game-controls-attrs)
     [radio-history]

     [:div.buttons
      (when (not= 0 time-scale)
        [:button {:on-click #(>evt [:game/set-time-scale 0])}
         "Pause"])

      [voice-controls]]]))
