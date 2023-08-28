(ns atc.game.traffic.shared-util
  (:require
   [clojure.string :as str]))

(defn partial-arrival-route [engine {:keys [route]}]
  (->>
    (str/split route #" ")
    (drop-last)
    (take-last 2)
    (mapcat (fn [id]
              (or (->> (get-in engine [:airport :arrivals id :path])
                       (map :fix)
                       (drop-last)
                       (seq))
                  (when (get-in engine [:game/navaids-by-id id])
                    [id]))))
    (distinct)
    (vec)))
