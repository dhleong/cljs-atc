(ns atc.game.traffic.shared-test
  (:require
   [atc.data.airports.kjfk :as kjfk]
   [atc.engine.model :refer [lateral-distance-to-squared]]
   [atc.game.traffic.shared :as shared :refer [partial-arrival-route
                                               position-arriving-aircraft]]
   [atc.subs :refer [navaids-by-id]]
   [atc.util.testing :refer [roughly=]]
   [cljs.test :refer-macros [deftest is testing]]
   [clojure.math :refer [sqrt]]))

(defn- create-engine []
  {:airport kjfk/airport
   :game/navaids-by-id (navaids-by-id kjfk/airport)})

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

(deftest position-arriving-aircraft-test
  (testing "Handle single-item route"
    ; NOTE: KBWI just ends at CAMRN
    (let [{[craft1 craft2 craft3] :crafts} (spawn-crafts-from
                                             "KBWI"
                                             "KBWI"
                                             "KBWI")]
      (is (roughly=
            @#'shared/lateral-spacing-m
            (aircraft-distance craft1 craft2)))
      (is (roughly=
            @#'shared/lateral-spacing-m
            (aircraft-distance craft2 craft3)))
      (is (roughly=
            (* 2 @#'shared/lateral-spacing-m)
            (aircraft-distance craft1 craft3)))))

  (testing "The second craft on a route should be laterally-spaced"
    (let [{[craft1 craft2] :crafts} (spawn-crafts-from
                                      "KBTV"
                                      "KBTV")]
      (is (roughly=
            @#'shared/lateral-spacing-m
            (aircraft-distance craft1 craft2)))))

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
            @#'shared/lateral-spacing-m
            (aircraft-distance craft1 craft2))))))
