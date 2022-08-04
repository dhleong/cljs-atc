(ns atc.core
  (:require
   ["react-dom/client" :rename {createRoot create-react-root}]
   [atc.events :as events]
   [atc.fx]
   [atc.routes :as routes]
   [atc.styles]
   [atc.subs]
   [atc.views :as views]
   [goog.dom :as gdom]
   [re-frame.core :as re-frame]
   [reagent.core :as r]))

(defonce ^:private root (create-react-root
                          (gdom/getElement "app")))

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (.render root (r/as-element [views/main])))

(defn ^:export init []
  (re-frame/dispatch-sync [::events/initialize-db])
  (routes/app-routes)
  (mount-root))

