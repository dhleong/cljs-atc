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

(defn spawn-crafts-from [& craft-specs]
  (let [engine (create-engine)
        [spec1 spec2] craft-specs

        craft1 (position-arriving-aircraft
                 engine
                 (merge (get-in kjfk/airport [:arrival-routes
                                              (:origin spec1)])
                        {:callsign (:callsign spec1)
                         :destination "KJFK"}))
        engine (assoc-in engine [:aircraft (:callsign craft1)] craft1)

        craft2 (position-arriving-aircraft
                 engine
                 (merge (get-in kjfk/airport [:arrival-routes
                                              (:origin spec1)])
                        {:callsign (:callsign spec2)
                         :destination "KJFK"}))]
    {:engine engine
     :crafts [craft1 craft2]}))

(deftest position-arriving-aircraft-test
  (testing "Handle single-item route"
    ; NOTE: KBWI just ends at CAMRN
    (let [{[craft1 craft2] :crafts} (spawn-crafts-from
                                     {:callsign "DAL1"
                                      :origin "KBWI"}
                                     {:callsign "DAL2"
                                      :origin "KBWI"})
          distance (sqrt
                     (lateral-distance-to-squared
                       (:position craft1)
                       (:position craft2)))]
      (is (roughly=
            @#'shared/lateral-spacing-m
            distance))))

  (testing "The second craft on a route should be laterally-spaced"
    (let [{[craft1 craft2] :crafts} (spawn-crafts-from
                                      {:callsign "DAL1"
                                       :origin "KBTV"}
                                      {:callsign "DAL2"
                                       :origin "KBTV"})
          distance (sqrt
                     (lateral-distance-to-squared
                       (:position craft1)
                       (:position craft2)))]
      (is (roughly=
            @#'shared/lateral-spacing-m
            distance))))

  (testing "The second craft on an overlapping route should be laterally-spaced"
    ; The LENDY8 and IGN1 routes end at LENDY
    (let [{[craft1 craft2] :crafts} (spawn-crafts-from
                                      {:callsign "DAL1"
                                       :origin "KDEN"}
                                      {:callsign "DAL2"
                                       :origin "KBUF"})
          distance (sqrt
                     (lateral-distance-to-squared
                       (:position craft1)
                       (:position craft2)))]
      (is (roughly=
            @#'shared/lateral-spacing-m
            distance)))))
