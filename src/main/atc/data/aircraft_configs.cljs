(ns atc.data.aircraft-configs
  (:require
   [atc.engine.config :as aircraft-config]))

(def common-jet (aircraft-config/create
                  ; NOTE: 3 degrees per second is a standard rate turn
                  ; see: https://en.wikipedia.org/wiki/Standard_rate_turn
                  {:turn-rate 3

                   :climb-rate 3000
                   :descent-rate 3500}))
