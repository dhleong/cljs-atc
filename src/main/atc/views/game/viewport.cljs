(ns atc.views.game.viewport
  (:require
   ["pixi-viewport" :refer [Viewport]]
   ["@inlet/react-pixi" :as px]
   [applied-science.js-interop :as j]))

(def PixiViewportComponent
  (px/PixiComponent
    "Viewport"
    #js {:create (j/fn [^:js {:keys [app plugins] :as js-props}]
                   (let [options (-> (js/Object.assign
                                       #js {:ticker (j/get app :ticker)
                                            :interaction (j/get-in app [:renderer :plugins :interaction])}
                                       js-props)

                                     ; NBD but we don't have a better way to remove these
                                     (j/assoc! :children js/undefined)
                                     (j/assoc! :app js/undefined))

                         viewport (Viewport. options)

                         render! (fn []
                                   (when-let [on-scale (j/get viewport :onScale)]
                                     (on-scale (j/get viewport :scaled)))
                                   (j/call-in app [:renderer :render] (j/get app :stage)))]

                     ; Enable plugins
                     (doseq [plugin plugins]
                       (j/call viewport plugin))

                     ; Stash this for use in the above callback:
                     ; NOTE: reagent "helpfully" converts to camel case here:
                     (j/assoc! viewport :onScale (j/get js-props :onScale))

                     ; Ensure render on move
                     ; FIXME: moved doesn't seem to be emitted on drag...
                     (.on viewport "moved" render!)
                     (.on viewport "zoomed" render!)

                     viewport))

         :applyProps (fn [viewport old-props new-props]
                       (doseq [prop (js/Object.keys new-props)]
                         (when-not (or (#{"plugins" "children" "app"} prop)
                                       (= (j/get new-props prop)
                                          (j/get old-props prop)))
                           (j/assoc! viewport (j/get new-props prop)))))}))

(defn- viewport-fn [props children]
  (let [app (px/useApp)]
    (into [:> PixiViewportComponent (assoc props :app app)]
          children)))

(defn viewport [& args]
  (if (map? (first args))
    [:f> viewport-fn (first args) (rest args)]
    [:f> viewport-fn {} args]))
