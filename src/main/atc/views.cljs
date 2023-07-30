(ns atc.views
  (:require
   [archetype.util :refer [<sub]]
   [archetype.views.error-boundary :refer [error-boundary]]
   [atc.views.game :as game]
   [atc.views.home :as home]
   [garden.units :refer [px]]
   [spade.core :refer [defglobal]]))

(defglobal global-vars
  [":root" {:*background* "#000"
            :*background-secondary* "#191d24"
            :*text* "#f4f7ff"}]
  [:button {:border-radius (px 4)
            :font-size :100%
            :padding [[(px 4) (px 8)]]}])

(def ^:private pages
  {:game #'game/view
   :home #'home/view})

(defn main []
  (let [[page args] (<sub [:page])
        page-form [(get pages page) args]]
    (println "[router]" page args page-form)

    [error-boundary
     page-form]))

