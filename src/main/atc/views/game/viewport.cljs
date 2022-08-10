(ns atc.views.game.viewport
  (:require
   ["pixi-viewport" :refer [Viewport]]
   ["@inlet/react-pixi" :as px]
   [applied-science.js-interop :as j]))

(defn- apply-center! [^Viewport viewport center]
  (when-not (= (j/get viewport :initial-center)
               center)
    (when center
      (j/assoc! viewport :initial-center center)
      (j/call viewport :moveCenter (j/get center :x) (j/get center :y))
      ; TODO how big should it actually be...?
      (j/call viewport :fitHeight (j/get viewport :worldHeight) true)
      (j/call viewport :render!))))

(def PixiViewportComponent
  (px/PixiComponent
    "Viewport"
    #js {:create (j/fn [^:js {:keys [app center plugins] :as js-props}]
                   (let [options (-> (js/Object.assign
                                       #js {:ticker (j/get app :ticker)
                                            :interaction (j/get-in app [:renderer :plugins :interaction])}
                                       js-props)

                                     ; NBD but we don't have a better way to remove these
                                     (j/assoc! :center js/undefined)
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

                     (j/assoc! viewport :render! render!)

                     (apply-center! viewport center)

                     ; Ensure render on move
                     ; FIXME: moved doesn't seem to be emitted on drag...
                     (.on viewport "moved" render!)
                     (.on viewport "zoomed" render!)

                     viewport))

         :applyProps (fn [viewport old-props new-props]
                       (doseq [prop (js/Object.keys new-props)]
                         (when-not (or (#{"plugins" "center" "children" "app"} prop)
                                       (= (j/get new-props prop)
                                          (j/get old-props prop)))
                           (j/assoc! viewport (j/get new-props prop)))
                         (apply-center! viewport (:center new-props))))}))

(defn- viewport-fn [props children]
  (let [app (px/useApp)]
    (into [:> PixiViewportComponent (assoc props :app app)]
          children)))

(defn viewport [& args]
  (if (map? (first args))
    [:f> viewport-fn (first args) (rest args)]
    [:f> viewport-fn {} args]))
