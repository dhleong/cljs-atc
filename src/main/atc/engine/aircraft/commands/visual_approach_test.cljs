(ns atc.engine.aircraft.commands.visual-approach-test
  (:require
   [atc.data.airports.kjfk :as kjfk]
   [atc.data.units :refer [ft->m nm->m]]
   [atc.engine.aircraft.commands.helpers :refer [primary-airport-position]]
   [atc.engine.aircraft.commands.visual-approach :refer [any-aircraft-on-leg?
                                                         can-see-airport?]]
   [atc.engine.core :refer [map->Engine]]
   [atc.engine.model :refer [command spawn-aircraft vec3]]
   [atc.util.testing :refer [create-engine]]
   [clojure.test :refer [deftest is testing]]))

(deftest can-see-airport?-test
  (testing "Don't break when there's *no* weather"
    (is (true? (can-see-airport?
                 {:position (vec3 0 (nm->m 8) (ft->m 2000))}
                 (primary-airport-position kjfk/airport)
                 {:wind-heading 200 :wind-kts 4}))))

  (testing "High visibility can see the airport"
    (is (true? (can-see-airport?
                 {:position (vec3 0 (nm->m 8) (ft->m 2000))}
                 (primary-airport-position kjfk/airport)
                 {:visibility-sm 10}))))

  (testing "Low visibility cannot see the airport"
    (is (false? (can-see-airport?
                  {:position (vec3 0 (nm->m 8) (ft->m 2000))}
                  (primary-airport-position kjfk/airport)
                  {:visibility-sm 2})))))

(deftest any-aircraft-on-leg?-test
  (testing "Detect aircraft on base leg"
    (let [engine (-> (create-engine)
                     (map->Engine)
                     (spawn-aircraft {:airline "DAL"
                                      :flight-number 22
                                      :destination "KJFK"
                                      :position {:x 0 :y 0}
                                      ; NOTE: Prevent trying to use voice:
                                      :pilot {}})
                     (command {:callsign "DAL22"
                               :instructions [[:cleared-approach :visual "13L"]]}
                              nil)
                     (update-in [:aircraft "DAL22" :behavior]
                                assoc
                                :visual-approach-state :base))]
      (is (seq (get-in engine [:aircraft "DAL22" :commands])))
      (is (seq (get-in engine [:aircraft "DAL22" :behavior])))
      (is (some?
            (any-aircraft-on-leg?
              engine "KJFK" "13L" :base))))))
