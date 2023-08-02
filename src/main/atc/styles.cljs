(ns atc.styles
  (:require
   [garden.units :refer [px]]
   [spade.core :refer [defclass defglobal]]))

(defglobal window-styles
  [":root" {:*background* "#000"
            :*background-secondary* "#191d24"
            :*text* "#f4f7ff"}]

  [:body {:margin 0
          :padding 0}]

  [:button {:border-radius (px 4)
            :font-size :100%
            :padding [[(px 4) (px 8)]]}
   [:&.destructive {:background-color "#aa3333"}]])

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
