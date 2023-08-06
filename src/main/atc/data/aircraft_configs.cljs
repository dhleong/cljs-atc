(ns atc.data.aircraft-configs
  (:require
   [atc.engine.config :as aircraft-config]))

(def common-jet (aircraft-config/create
                  ; NOTE: 3 degrees per second is a standard rate turn
                  ; see: https://en.wikipedia.org/wiki/Standard_rate_turn
                  {:type "B737"

                   :turn-rate 3

                   :climb-rate 3000
                   :descent-rate 3500
                   :acceleration 7
                   :deceleration 3

                   :min-speed 110
                   :cruise-speed 460
                   :landing-speed 125}))
