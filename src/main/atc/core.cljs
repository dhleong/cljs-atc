(ns atc.core
  (:require
    [goog.dom :as gdom]
    [reagent.dom :as rdom]
    [re-frame.core :as re-frame]
    [atc.events :as events]
    [atc.routes :as routes]
    [atc.views :as views]
    [atc.fx]
    [atc.styles]
    [atc.subs]
    [atc.speech :as speech]))

; (defonce ^:private root (create-react-root
;                           (gdom/getElement "app")))

; (defn ^:dev/after-load mount-root []
;   (re-frame/clear-subscription-cache!)
;   (.render root (r/as-element [views/main])))

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (speech/init)
  (rdom/render [views/main]
               (gdom/getElement "app")))

(defn ^:export init []
  (re-frame/dispatch-sync [::events/initialize-db])
  (routes/app-routes)
  (mount-root))

