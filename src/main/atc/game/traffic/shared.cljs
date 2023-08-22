(ns atc.game.traffic.shared
  (:require
   [atc.data.core :refer [local-xy]]
   [atc.data.units :refer [nm->m]]
   [atc.engine.model :refer [bearing-to->vec distance-to-squared normalize v*]]
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

(def ^:private lateral-spacing-m (nm->m 5))
(def ^:private lateral-spacing-m-squared (* lateral-spacing-m lateral-spacing-m))

(defn distribute-crafts-along-route [engine route crafts]
  (letfn [(position-of [item]
            (local-xy item (:airport engine)))]
    (loop [results [(assoc (first crafts)
                           :position (get-in engine [:game/navaids-by-id
                                                     (last route)
                                                     :position]))]
           navaids (next (reverse route))
           crafts (next crafts)]
      (if-not (seq crafts)
        results

        (let [next-navaid-id (first navaids)
              next-navaid (get-in engine [:game/navaids-by-id next-navaid-id])
              distance-to-next-navaid-sq (distance-to-squared
                                           (position-of (peek results))
                                           (position-of next-navaid))]
          (println next-navaid-id distance-to-next-navaid-sq)
          (if (>= distance-to-next-navaid-sq lateral-spacing-m-squared)
            ; Plenty of room along the current radial
            (let [bearing-to-previous (bearing-to->vec
                                        (position-of (peek results))
                                        (position-of next-navaid))
                  new-position (v* (normalize bearing-to-previous)
                                   lateral-spacing-m)]
              (recur
                (conj results
                      (assoc (first crafts)
                             :position new-position))
                navaids
                (next crafts)))

            ; TODO No more room between the last aircraft and the next navaid.
            ; If there's another navaid we need to "turn the corner". If
            ; not, we need to just extend along the current bearing
            results))))))
