(ns atc.game.traffic.shared
  (:require
   [atc.data.core :refer [local-xy]]
   [atc.data.units :refer [nm->m]]
   [atc.engine.model :refer [bearing-to bearing-to->vec
                             lateral-distance-to-squared normalize v* v+ vec3 Vec3]]
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
  (letfn [(position-of [{:keys [position]}]
            (if (instance? Vec3 position)
              position
              (local-xy position (:airport engine))))]
    (loop [results [(let [navaid-pos (position-of
                                       (get-in engine [:game/navaids-by-id
                                                       (last route)]))]
                      (assoc (first crafts)
                             ; TODO Follow arrival route?
                             :heading (bearing-to navaid-pos {:x 0 :y 0})
                             :position (vec3
                                         navaid-pos
                                         ; TODO ?
                                         180000)))]
           navaids (next (reverse route))
           crafts (next crafts)]
      (if-not (seq crafts)
        results

        (let [next-navaid-id (first navaids)
              next-navaid (get-in engine [:game/navaids-by-id next-navaid-id])
              distance-to-next-navaid-sq (lateral-distance-to-squared
                                           (position-of (peek results))
                                           (position-of next-navaid))]
          (if (>= distance-to-next-navaid-sq lateral-spacing-m-squared)
            ; Plenty of room along the current radial
            (let [bearing-to-previous (bearing-to->vec
                                        (vec3 (position-of (peek results)) 0)
                                        (vec3 (position-of next-navaid) 0))
                  new-position (v+
                                 (position-of (peek results))
                                 (v* (normalize bearing-to-previous)
                                     lateral-spacing-m))]
              (recur
                (conj results
                      (assoc (first crafts)
                             ; TODO Command to continue arrival
                             :heading (bearing-to new-position next-navaid)
                             :position (vec3 new-position
                                             ; TODO: What should the altitude be here?
                                             180000)))
                navaids
                (next crafts)))

            ; TODO No more room between the last aircraft and the next navaid.
            ; If there's another navaid we need to "turn the corner". If
            ; not, we need to just extend along the current bearing
            results))))))
