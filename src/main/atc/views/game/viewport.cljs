(ns atc.views.game.viewport
  (:require
   ["pixi-viewport" :refer [Viewport]]
   ["@inlet/react-pixi" :as px]
   [applied-science.js-interop :as j]))

(def PixiViewportComponent
  (px/PixiComponent
    "Viewport"
    #js {:create (j/fn [^:js {:keys [app plugins] :as _props}]
                   ; TODO: pull other viewport props from _props
                   (let [viewport (Viewport.
                                    #js {:ticker (j/get app :ticker)
                                         :interaction (j/get-in app [:renderer :plugins :interaction])})
                         render! (fn []
                                   (j/call-in app [:renderer :render] (j/get app :stage)))]

                     ; enable plugins
                     (doseq [plugin plugins]
                       (j/call viewport plugin))

                     ; Ensure render on move
                     ; FIXME: moved doesn't seem to be emitted on drag...
                     (.on viewport "moved" render!)
                     (.on viewport "zoomed" render!)

                     viewport))

         :applyProps (fn [_viewport _old-props _new-props]
                       ; TODO remove plugins, children; set changed props on viewport instance
                       #_(let [old-]))

         :didMount #(println "Mounted viewport")}))

(defn- viewport-fn [props children]
  (let [app (px/useApp)]
    (into [:> PixiViewportComponent (assoc props :app app)]
          children)))

(defn viewport [& args]
  (if (map? (first args))
    [:f> viewport-fn (first args) (rest args)]
    [:f> viewport-fn {} args]))
