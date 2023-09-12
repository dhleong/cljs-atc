(ns atc.components.icon-button
  (:require
   [garden.units :refer [px]]
   [spade.core :refer [defclass]]))

(defn- prepend-class [old-class new-class]
  (println "prepend" new-class " to " old-class)
  (cond
    (nil? old-class) new-class
    (coll? old-class) (cons new-class old-class)
    :else [new-class old-class]))

(defn- prevent-default [f]
  (when f
    (fn [e]
      (.preventDefault e)
      (f e))))

(defclass icon-button-class []
  {:display :inline-flex
   :align-items :center
   :border-radius (px 4)
   :color :*text*
   :cursor :pointer
   :justify-content :center
   :padding (px 8)}
  [:&:hover {:background-color :*background-hover*}])

(defn icon-button [opts content]
  [:a (-> opts
          (update :class prepend-class (icon-button-class))
          (update :on-click prevent-default))
   content])
