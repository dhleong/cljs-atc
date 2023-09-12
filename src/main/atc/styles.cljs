(ns atc.styles
  (:require
   [atc.theme :as theme]
   [garden.units :refer [px]]
   [spade.core :refer [defclass defglobal]]))

(defglobal window-styles
  [":root" {:*accent* theme/aircraft-tracked-obj
            :*background* theme/background
            :*background-secondary* theme/background-secondary
            :*text* theme/text
            :*map-text* theme/map-label-opaque}]

  [:body {:margin 0
          :padding 0}]

  [:button {:border-radius (px 4)
            :font-size :100%
            :padding [[(px 4) (px 8)]]}
   [:&.destructive {:background-color theme/action-destructive}]])

(defclass full-screen [& [{:keys [position center-content? justify-content]
                           :or {position :absolute
                                center-content? true
                                justify-content :center}}]]
  (merge
    {:position position
     :left 0
     :right 0
     :top 0
     :bottom 0}
    (when center-content?
      {:display :flex
       :align-items :center
       :justify-content justify-content})))
