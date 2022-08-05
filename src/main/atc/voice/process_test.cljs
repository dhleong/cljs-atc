(ns atc.voice.process-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [atc.voice.process :refer [find-command]]))

(defn find-instructions [input]
  (-> input
      find-command
      :instructions))

(deftest find-command-test
  (testing "Simple nop with callsign"
    (is (= {:callsign "N1"
            :instructions [[:standby]]}
           (find-command "piper one standby"))))

  (testing "Airline callsigns"
    (is (= "BAW4251"
           (:callsign
             (find-command "speed bird fourty two fifty one standby")))))

   (testing "Multiple instructions"
    (is (= {:callsign "N2"
            :instructions [[:adjust-altitude 11000]
                           [:contact-other :tower]]}
           (find-command "piper two climb maintain one one thousand contact tower"))))

   (testing "Handle unparseable input gracefully"
     (let [instructions (find-instructions
                          "piper one fly heading two three four have fun")]
       (is (= [[:steer 234]]
              (pop instructions)))
       (is (= :error (-> instructions last first))))))

(deftest steer-test
  (testing "Process steer direction"
    (is (= [[:steer 985 :right]]
           (find-instructions
             "piper one turn right heading niner eight fife")))))
