(ns atc.game.traffic.shared
  (:require
   [clojure.string :as str]))

(defn partial-arrival-route [airport {:keys [route]}]
  (->>
    (str/split route #" ")
    (drop-last)
    (take-last 2)
    (mapcat (fn [id]
              (or (map :fix
                       (get-in airport [:arrivals id :path]))
                  [id])))
    distinct
    drop-last))

(defn space-crafts-along-route [engine route crafts]
  (let [last-navaid (last route)]
    [(assoc (first crafts)
            :position (get-in engine [:game/navaids-by-id last-navaid :position]))]))
