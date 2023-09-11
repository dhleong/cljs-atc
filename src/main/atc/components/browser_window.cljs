(ns atc.components.browser-window
  (:require
   ["react-dom" :as react-dom]
   [applied-science.js-interop :as j]
   [atc.styles :refer [window-styles]]
   [clojure.string :as str]
   [reagent.core :as r]
   [spade.react :as spade-react]
   [spade.runtime :as spade-runtime]))

; NOTE: This is a hack! spade doesn't currently support automatically
; rendering global styles into a style-container
(defn- window-styles-mounter []
  #_{:clj-kondo/ignore [:invalid-arity]} ; Kondo is wrong here...?
  (spade-runtime/ensure-style!
    :global
    (meta #'window-styles)
    (constantly "window-styles")
    (constantly {:css window-styles})
    nil)
  [:<>])

(defn- open-window [{:keys [window-name] :as opts}]
  (let [w (js/window.open
            ""
            (str window-name)
            (->> (reduce-kv
                   (fn [s k v]
                     (cond-> s
                       (not= k window-name)
                       (conj (str (name k) "=" v))))
                   []
                   opts)
                 (str/join ",")))]

    (.appendChild
      (j/get-in w [:document :head])
      (doto (j/call-in w [:document :createElement] "meta")
        (.setAttribute "name" "darkreader-lock")))

    (.appendChild
      (j/get-in w [:document :head])
      (doto (j/call-in w [:document :createElement] "meta")
        (.setAttribute "name" "darkreader")
        (.setAttribute "content" (str "window-" window-name))))

    w))

(defn- browser-window-root [window children]
  (into [spade-react/with-dom (j/get-in window [:document :head])
         [window-styles-mounter]]
        children))

; NOTE: If we don't track the window externally to the browser component,
; the window will get destroyed and reopened every time we hot swap
(defonce ^:private windows (atom {}))

(defn- mount-configured-window [state config]
  (letfn [(closed? [w]
            (or (nil? w)
                (j/get w :closed)))]
    (cond-> state
      (closed? (get-in state [config :window]))
      (assoc config {:window (open-window config)})

      true
      (update-in [config :mounted] inc))))

(defn- unmount-configured-window [state config]
  (update-in state [config :mounted] dec))

(defn browser-window [config & children]
  (r/with-let [state (swap! windows mount-configured-window config)
               window (get-in state [config :window])]

    (react-dom/createPortal
      (r/as-element [browser-window-root window children])
      (j/get-in window [:document :body]))

    (finally
      (let [state (swap! windows unmount-configured-window config)]
        (when (= 0 (get-in state [config :mounted]))
          (.close window))))))
