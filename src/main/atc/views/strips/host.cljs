(ns atc.views.strips.host
  (:require
   ["react-dom" :as react-dom]
   [applied-science.js-interop :as j]
   [archetype.util :refer [<sub]]
   [atc.styles :refer [window-styles]]
   [atc.views.strips.subs :as subs]
   [reagent.core :as r]
   [spade.core :refer [defattrs]]
   [spade.react :as spade-react]
   [spade.runtime :as spade-runtime]))

; NOTE: This is a hack! spade doesn't currently support automatically
; rendering global styles into a style-container
(defn window-styles-mounter []
  #_{:clj-kondo/ignore [:invalid-arity]} ; Kondo is wrong here...?
  (spade-runtime/ensure-style!
    :global
    (meta #'window-styles)
    (constantly "window-styles")
    (constantly {:css window-styles})
    nil))

(defattrs flight-strips-attrs []
  {:background "red"})

(defn flight-strips []
  [window-styles-mounter]
  [:div (flight-strips-attrs)
   (str (<sub [:game/aircraft]))])

(defn- flight-strips-root []
  (let [window (<sub [::subs/window])]
    [spade-react/with-dom (j/get-in window [:document :head])
     [flight-strips]]))

(defn flight-strips-host []
  (when-let [window (<sub [::subs/window])]
    (react-dom/createPortal
      (r/as-element [flight-strips-root])
      (j/get-in window [:document :body]))))
