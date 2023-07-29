(ns atc.core
  (:require
    [reagent.dom.client :as rdom]
    [atc.events :as events]
    [atc.routes :as routes]
    [atc.views :as views]
    [atc.fx]
    [atc.styles]
    [atc.subs]
    [atc.speech :as speech]
    [goog.dom :as gdom]
    [re-frame.core :as re-frame]
    [re-pressed.core :as rp]
    [spade.runtime :refer [*css-compile-flags*]]))

(set!
  *css-compile-flags*
  (assoc
    *css-compile-flags*
    :vendors [:webkit]
    :auto-prefix #{:line-clamp
                   :user-select}))

(defonce ^:private root (rdom/create-root
                          (gdom/getElement "app")))

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (speech/init)
  (rdom/render root [views/main]))

(defn ^:export init []
  (re-frame/dispatch-sync [::events/initialize-db])
  (re-frame/dispatch-sync [::rp/add-keyboard-event-listener "keydown"])
  (re-frame/dispatch-sync [::rp/add-keyboard-event-listener "keyup"])
  (re-frame/dispatch-sync [::rp/add-keyboard-event-listener "keypress"])
  (routes/app-routes)
  (mount-root))

