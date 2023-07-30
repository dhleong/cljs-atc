(ns atc.views.pause-screen
  (:require
   [spade.core :refer [defattrs]]))

(defattrs pause-screen-attrs []
  {:background-color :*background-secondary*
   :color :*text*
   :display :flex
   :align-items :center
   :justify-content :center
   :opacity 0.5
   :pointer-events :none
   :position :fixed
   :left 0
   :right 0
   :top 0
   :bottom 0})

(defn view []
  [:div (pause-screen-attrs)
   [:div.contents
    "PAUSED"]])
