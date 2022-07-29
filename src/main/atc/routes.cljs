(ns atc.routes
  (:require-macros [secretary.core :refer [defroute]])
  (:require [secretary.core :as secretary]
            [archetype.nav :as nav :refer [navigate!]]))

(defn- def-routes []
  (secretary/reset-routes!)

  ;;
  ;; app routes declared here:

  (defroute "/" []
    (navigate! :home)))

(defn app-routes []
  (nav/init!)

  (def-routes)

  (nav/hook-browser-navigation!))

