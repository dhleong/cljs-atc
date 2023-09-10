(ns atc.views.strips.host
  (:require
   ["react-dom" :as react-dom]
   [applied-science.js-interop :as j]
   [archetype.util :refer [<sub]]
   [atc.views.strips.subs :as subs]
   [reagent.core :as r]))

(defn flight-strips []
  [:div (str (<sub [:game/aircraft]))])

(defn flight-strips-host []
  (when-let [window (<sub [::subs/window])]
    (println "PORTAL INTO " window)
    (react-dom/createPortal
      (r/as-element [flight-strips])
      (j/get-in window [:document :body]))))
