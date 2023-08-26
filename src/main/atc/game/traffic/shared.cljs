(ns atc.game.traffic.shared
  (:require
   [atc.data.core :refer [local-xy]]
   [atc.data.units :refer [ft->m nm->m]]
   [atc.engine.model :refer [bearing-to bearing-to->vec bearing-vec->degrees
                             lateral-distance-to-squared normalize v* v+ vec3
                             Vec3]]
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
    (distinct)))

(def ^:private lateral-spacing-m (nm->m 5))
(def ^:private lateral-spacing-m-squared (* lateral-spacing-m lateral-spacing-m))

(defn- spawn-craft [craft {:keys [heading position]}]
  (assoc craft
         ; TODO Follow arrival route?
         :heading heading
         :position (vec3
                     position
                     ; TODO altitude?
                     (ft->m 18000))))

(defn distribute-crafts-along-route [engine route crafts]
  (letfn [(position-of [{:keys [position]}]
            (if (instance? Vec3 position)
              position
              (local-xy position (:airport engine))))]
    (loop [results [(let [navaid-pos (position-of
                                       (get-in engine [:game/navaids-by-id
                                                       (last route)]))]
                      (spawn-craft
                        (first crafts)
                        {:heading (bearing-to navaid-pos {:x 0 :y 0})
                         :position navaid-pos}))]
           navaids (next (reverse route))
           crafts (next crafts)]
      (cond
        (not (seq crafts))
        results

        ; No more navaids; just continue along the last bearing
        (not (seq navaids))
        (let [final-craft (peek results)
              penultimate-craft (peek (pop results))
              bearing (bearing-to->vec
                        (vec3 (position-of penultimate-craft) 0)
                        (vec3 (position-of final-craft) 0))
              new-position (v+
                             (position-of (peek results))
                             (v* (normalize bearing)
                                 lateral-spacing-m))]
          (recur
            (conj results
                  (spawn-craft
                    (first crafts)
                    {:heading (+ 180 (bearing-vec->degrees bearing))
                     :position new-position}))
            navaids
            (next crafts)))

        :else
        (let [next-navaid-id (first navaids)
              next-navaid (get-in engine [:game/navaids-by-id next-navaid-id])
              distance-to-next-navaid-sq (lateral-distance-to-squared
                                           (position-of (peek results))
                                           (position-of next-navaid))]
          (if (>= distance-to-next-navaid-sq lateral-spacing-m-squared)
            ; Plenty of room along the current radial
            ; TODO The previous craft might not actually be on the radial.
            ; If that's the case, we need to project the new position onto
            ; the radial
            (let [bearing-to-previous (bearing-to->vec
                                        (vec3 (position-of (peek results)) 0)
                                        (vec3 (position-of next-navaid) 0))
                  new-position (v+
                                 (position-of (peek results))
                                 (v* (normalize bearing-to-previous)
                                     lateral-spacing-m))]
              (recur
                (conj results
                      (spawn-craft
                        (first crafts)
                        {:heading (+ 180 (bearing-to new-position next-navaid))
                         :position new-position}))
                navaids
                (next crafts)))

            ; TODO No more room between the last aircraft and the next navaid.
            ; If there's another navaid we need to "turn the corner". If
            ; not, we need to just extend along the current bearing
            (recur
              results
              (next navaids)
              crafts)))))))
