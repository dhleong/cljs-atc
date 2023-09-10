(ns atc.util.fetch
  (:require
   [applied-science.js-interop :as j]
   [promesa.core :as p]))

(defn- fetch-with-timeout-impl [response-type url]
  (p/let [controller (js/AbortController.)
          timeout-ms 4500
          timeout-timer (js/setTimeout #(.abort controller) timeout-ms)
          response (js/fetch url #js {:signal (j/get controller :signal)})
          result (j/call response response-type)]
    (js/clearTimeout timeout-timer)
    result))

(def fetch-with-timeout
  (if (j/get js/window :AbortController)
    fetch-with-timeout-impl
    js/fetch))
