(ns atc.core
  (:require [reagent.dom :as rdom]
            [re-frame.core :as re-frame]
            [atc.events :as events]
            [atc.routes :as routes]
            [atc.views :as views]
            [atc.fx]
            [atc.styles]
            [atc.subs]))

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (rdom/render [views/main]
               (.getElementById js/document "app")))

(defn ^:export init []
  (re-frame/dispatch-sync [::events/initialize-db])
  (routes/app-routes)
  (mount-root))

