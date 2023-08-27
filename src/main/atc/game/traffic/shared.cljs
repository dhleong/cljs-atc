(ns atc.game.traffic.shared
  (:require
   [atc.config :as config]
   [atc.data.core :refer [local-xy]]
   [atc.data.units :refer [ft->m nm->m]]
   [atc.engine.model :refer [bearing-to distance-to-squared dot* normalize v*
                             v- vec3 Vec3 vmag vmag2]]
   [atc.util.coll :refer [min-by]]
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

(def ^:private lateral-spacing-m (nm->m 5))

(defn- spawn-craft [craft {:keys [heading position]}]
  (assoc craft
         ; TODO Follow arrival route?
         :heading heading
         :position (vec3
                     position
                     ; TODO altitude?
                     (ft->m 18000))))

(defn- engine-arrivals [{:keys [aircraft airport]}]
  (->> aircraft vals (filter #(= (:id airport) (:destination %)))))

(defn- approx-distance-to-segment [point [p1 p2]]
  ; FROM: https://en.m.wikipedia.org/wiki/Distance_from_a_point_to_a_line
  (abs (/ (- (* (- (:x p2) (:x p1))
                (- (:y p1) (:y point)))
             (* (- (:x p1) (:x point))
                (- (:y p2) (:y p1))))

          ; For precise distance we should sqrt this, but we don't need precise!
          (distance-to-squared p1 p2))))

(defn- project-point-onto-segment [point [a b]]
  ; From: http://www.sunshine2k.de/coding/java/PointOnLine/PointOnLine.html
  (let [e1 (v- b a)
        e2 (v- point a)
        dot-prod (dot* e1 e2)
        squared-len (vmag2 e1)]
    (vec3
      (+ (:x a) (/ (* dot-prod (:x e1))
                   squared-len))
      (+ (:y a) (/ (* dot-prod (:y e1))
                   squared-len))
      0)))

(defn- compute-initial-position [{:keys [initializing?
                                         arrivals-by-route
                                         arrivals-on-my-route
                                         first-pos-on-my-route
                                         last-pos-on-my-route]}]
  ; If this aircraft is the first on its route *and* we're still
  ; initializing the game, we apply a special case where we
  ; round-robin distances from last navaid on route to not
  ; overwhelm the controller with a bunch of simultaneous arrivals
  (let [distance-from-previous (if-not (or (seq arrivals-on-my-route)
                                           (not initializing?))
                                 (* (count arrivals-by-route)
                                    lateral-spacing-m)
                                 (* (count arrivals-on-my-route)
                                    lateral-spacing-m))

        base-distance (max
                        (vmag last-pos-on-my-route)

                        ; Make sure we don't spawn inside the control range
                        (+ config/ctr-control-radius-m
                           (nm->m 5)))

        ; For spawning the initial arrivals, we choose a position some
        ; distance from the airport in the direction of the route
        rough-initial-distance (+ base-distance
                                  distance-from-previous)

        spawn-distance (max
                         ; Always use the rough distance at init
                         rough-initial-distance

                         ; Otherwise, make sure we're *at least* as
                         ; far as the furthest fix in the path, to
                         ; try to avoid pop-in
                         (when-not initializing?
                           (vmag first-pos-on-my-route)))]
    (v*
      (normalize last-pos-on-my-route)
      spawn-distance)))

(defn position-arriving-aircraft
  "Position an arriving aircraft somewhere along its arrival route,
   behind any existing aircraft in `engine` using that same route"
  [engine craft]
  (letfn [(position-of [{:keys [position]}]
            (if (instance? Vec3 position)
              position
              (vec3 (local-xy position (:airport engine)))))

          (navaid-by-id [id]
            (or (get-in engine [:game/navaids-by-id id])
                (throw (ex-info (str "No such navaid: " id) {:id id}))))]
    (let [my-route (partial-arrival-route engine craft)
          my-final-navaid (last my-route)
          all-arrivals (engine-arrivals engine)
          arrivals-by-route (group-by
                              (comp last
                                    (partial partial-arrival-route engine))
                              all-arrivals)

          ; First, pick a rough position where we want this craft
          approx-position (compute-initial-position
                            {:initializing? (= 0 (:elapsed-s engine 0))
                             :arrivals-by-route arrivals-by-route
                             :arrivals-on-my-route (get arrivals-by-route
                                                        my-final-navaid)
                             :first-pos-on-my-route (position-of
                                                      (navaid-by-id
                                                        (first my-route)))
                             :last-pos-on-my-route (position-of
                                                     (navaid-by-id
                                                       (last my-route)))})

          ; Next, find the closest segment on the route to this point
          route-positions (->> my-route
                               (reverse)
                               (map (comp position-of navaid-by-id))
                               (cons (vec3 {:x 0 :y 0})))
          route-segments (map vector
                              (next route-positions)
                              route-positions)
          closest-segment (->> route-segments
                               (min-by (partial
                                         approx-distance-to-segment
                                         approx-position)))

          ; Now, project our approximate position *onto* that segment
          ; TODO: If there are any aircraft that are too close, push
          ; further backward...
          projected-position (project-point-onto-segment
                               approx-position
                               closest-segment)

          ; Head to the starting point of the closest segment
          ; NOTE: the second (last) item of the segment will be the closest
          ; to the airport
          heading (bearing-to
                    projected-position
                    (peek closest-segment))]
      (spawn-craft
        craft
        {:position projected-position
         :heading heading}))))
