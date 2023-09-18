(ns atc.game.traffic.shared-test
  (:require
   [atc.config :as config]
   [atc.data.airports.kjfk :as kjfk]
   [atc.engine.model :refer [distance-to-squared lateral-distance-to-squared]]
   [atc.game.traffic.shared :as shared :refer [grouping-navaid-of-route
                                               position-arriving-aircraft]]
   [atc.game.traffic.shared-util :refer [partial-arrival-route]]
   [atc.util.testing :refer [create-engine roughly=]]
   [cljs.test :refer-macros [deftest is testing]]
   [clojure.math :refer [sqrt]]))

(defn- aircraft-distance [craft1 craft2]
  (sqrt
    (lateral-distance-to-squared
      (:position craft1)
      (:position craft2))))

(deftest partial-arrival-route-test
  (testing "Extract a partial arrival route"
    (is (= ["ENE" "PARCH" "CCC" "ROBER"]
           (partial-arrival-route
             (create-engine)
             {:route (get-in kjfk/airport [:arrival-routes "KBTV" :route])})))

    ; Handle navigating direct to a fix
    (is (= ["CAMRN"]
           (partial-arrival-route
             (create-engine)
             {:route (get-in kjfk/airport [:arrival-routes "KBWI" :route])})))))

(defn spawn-crafts-from [& origins]
  (loop [engine (create-engine)
         origins origins
         crafts []]
    (if-not (seq origins)
      {:engine engine
       :crafts crafts}

      (let [origin (first origins)
            craft (position-arriving-aircraft
                    engine
                    (merge (get-in kjfk/airport [:arrival-routes origin])
                           {:callsign (str "DAL" (inc (count crafts)))
                            :destination "KJFK"}))]
        (recur
          (assoc-in engine [:aircraft (:callsign craft)] craft)
          (next origins)
          (conj crafts craft))))))

(defn- assert-no-overlap [crafts]
  (doall
    (for [c1 crafts
          c2 crafts]
      (when-not (== c1 c2)
        (let [engine (create-engine)
              c1-partial-route (partial-arrival-route engine c1)
              c1-grouping (grouping-navaid-of-route engine c1-partial-route)
              c2-partial-route (partial-arrival-route engine c2)
              c2-grouping (grouping-navaid-of-route engine c2-partial-route)
              delta (/ config/arrivals-lateral-spacing-m 2)]
          (assert (not (roughly= (:position c1) (:position c2)
                                 :delta delta))
                  (str "\nOVERLAP! " (sqrt (distance-to-squared
                                             (:position c1)
                                             (:position c2)))  "m\n"
                       " " (:callsign c1) " (" (:route c1) ")"
                       "\n -> " c1-partial-route " @ " c1-grouping "\n"
                       "COLLIDED with:\n"
                       " " (:callsign c2) " (" (:route c2) ")"
                       "\n -> " c2-partial-route " @ " c2-grouping "\n"
                       "same navaid? " (= c1-grouping c2-grouping))))))))

(deftest position-arriving-aircraft-test
  (testing "Handle single-item route"
    ; NOTE: KBWI just ends at CAMRN
    (let [{[craft1 craft2 craft3] :crafts} (spawn-crafts-from
                                             "KBWI"
                                             "KBWI"
                                             "KBWI")]
      (is (roughly=
            config/arrivals-lateral-spacing-m
            (aircraft-distance craft1 craft2)
            :delta 250))
      (is (roughly=
            config/arrivals-lateral-spacing-m
            (aircraft-distance craft2 craft3)
            :delta 350))
      (is (roughly=
            (* 2 config/arrivals-lateral-spacing-m)
            (aircraft-distance craft1 craft3)
            :delta 450))))

  (testing "The second craft on a route should be laterally-spaced"
    (let [{[craft1 craft2] :crafts} (spawn-crafts-from
                                      "KBTV"
                                      "KBTV")]
      (is (roughly=
            config/arrivals-lateral-spacing-m
            (aircraft-distance craft1 craft2)
            :delta 250))))

  (testing "The second craft on an overlapping route should be laterally-spaced"
    ; The LENDY8 and IGN1 routes end at LENDY
    (let [{crafts :crafts} (spawn-crafts-from
                             "KDEN"
                             "KBUF"
                             "KDEN"
                             "KBUF")
          [craft1 craft2] crafts]
      (is (= 4 (count (distinct (map :position crafts)))))
      (is (roughly=
            config/arrivals-lateral-spacing-m
            (aircraft-distance craft1 craft2)
            :delta 250))))

  (testing "No overlap, for reals"
    ; A sample random set of arrivals from a real app run that caused one
    ; overlap
    (let [{crafts :crafts} (spawn-crafts-from
                             "KCRQ" "KBUR" "KPHX" "KSMO" "KFMY"
                             "KRDU" "CYYZ" "KRDU" "KORD" "KTRM")]
      (assert-no-overlap crafts)
      (is (= 10 (count (distinct (map :position crafts)))))))

  (testing "No overlap, for reals - pt2"
    ; A sample random set of arrivals from a real app run that caused one
    ; overlap
    (let [{crafts :crafts} (spawn-crafts-from
                             "KRSW" "KHYA" "KCVG" "KCRQ" "KLAX"
                             "KPWM" "KORF" "KIAD" "KSAT" "KIND")]
      (assert-no-overlap crafts)
      (is (= 10 (count (distinct (map :position crafts))))))))
