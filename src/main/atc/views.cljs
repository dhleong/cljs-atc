(ns atc.views
  (:require
   [archetype.util :refer [<sub]]
   [archetype.views.error-boundary :refer [error-boundary]]
   [atc.views.game :as game]
   [atc.views.home :as home]
   [atc.views.strips.host :refer [flight-strips-host]]))

(def ^:private pages
  {:game #'game/view
   :home #'home/view})

(defn main []
  (let [[page args] (<sub [:page])
        page-form [(get pages page) args]]
    (println "[router]" page args page-form)

    [error-boundary
     page-form
     [flight-strips-host]]))

