(ns atc.views
  (:require [archetype.views.error-boundary :refer [error-boundary]]
            [archetype.util :refer [<sub]]
            [atc.views.home :as home]))

(def ^:private pages
  {:home #'home/view})

(defn main []
  (let [[page args] (<sub [:page])
        page-form [(get pages page) args]]
    (println "[router]" page args page-form)

    [error-boundary
     page-form]))

