(ns atc.engine.aircraft.commands.visual-approach-test
  (:require
   [atc.data.airports.kjfk :as kjfk]
   [atc.data.units :refer [ft->m nm->m]]
   [atc.engine.aircraft.commands.helpers :refer [primary-airport-position]]
   [atc.engine.aircraft.commands.visual-approach :refer [can-see-airport?]]
   [atc.engine.model :refer [vec3]]
   [clojure.test :refer [deftest is testing]]))

(deftest can-see-airport?-test
  (testing "Don't break when there's *no* weather"
    (is (true? (can-see-airport?
                 {:position (vec3 0 (nm->m 8) (ft->m 2000))}
                 (vec3 0 0 (ft->m (last (:position kjfk/airport))))
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
