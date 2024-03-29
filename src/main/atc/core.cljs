(ns atc.core
  (:require
    ["@pixi/math-extras"]
    [goog.dom :as gdom]
    [reagent.dom :as rdom]
    [re-frame.core :as re-frame]
    [re-pressed.core :as rp]
    [atc.events :as events]
    [atc.routes :as routes]
    [atc.views :as views]
    [atc.fx]
    [atc.styles]
    [atc.subs]
    [atc.speech :as speech]
    [spade.runtime :refer [*css-compile-flags*]]))

(set!
  *css-compile-flags*
  (assoc
    *css-compile-flags*
    :vendors [:webkit]
    :auto-prefix #{:line-clamp
                   :user-select}))

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
  (re-frame/dispatch-sync [::rp/add-keyboard-event-listener "keydown"])
  (re-frame/dispatch-sync [::rp/add-keyboard-event-listener "keyup"])
  (re-frame/dispatch-sync [::rp/add-keyboard-event-listener "keypress"])
  (routes/app-routes)
  (mount-root))

