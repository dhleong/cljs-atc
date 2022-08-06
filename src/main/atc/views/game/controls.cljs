(ns atc.views.game.controls
  (:require
   [archetype.util :refer [<sub >evt]]
   ["react" :as React]
   [spade.core :refer [defattrs]]))

(defn- voice-controls-active []
  (React/useEffect
    (fn []
      (>evt [:voice/enable-keypresses])
      #(>evt [:voice/disable-keypresses])))

  [:<>
   [:button {:on-mouse-down #(>evt [:voice/set-paused false])
             :on-mouse-up #(>evt [:voice/set-paused true])}
    "MIC (hold this or space)"]
   (<sub [:voice/partial])])

(defn- voice-controls []
  (let [state (<sub [:voice/state])]
    (case state
      nil [:button {:on-click #(>evt [:voice/start!])}
           "Enable Microphone"]
      :ready [:f> voice-controls-active]
      [:button {:disabled true}
       "(" (name state) ")"])))

(defattrs game-controls-attrs []
  {:display :flex
   :flex-direction :row})

(defn game-controls []
  (let [time-scale (<sub [:game/time-scale])
        game-running? (some? time-scale)]
    [:div (game-controls-attrs)
     (if game-running?
       [:button {:on-click #(>evt [:game/reset])}
        "End Game"]
       [:button {:on-click #(>evt [:game/init])}
        "Init Game"])

     (when game-running?
       (if (= 0 time-scale)
         [:button {:on-click #(>evt [:game/set-time-scale 1])}
          "Resume"]
         [:button {:on-click #(>evt [:game/set-time-scale 0])}
          "Pause"]))

     [voice-controls]]))
