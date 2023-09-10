(ns atc.voice.process-test
  (:require
   [atc.data.airports.kjfk :as kjfk]
   [atc.voice.process :rename {find-command actual-find-command} :refer [build-machine]]
   [cljs.test :refer-macros [deftest is testing]]))

(def machine
  (delay
    (build-machine
      (:navaid-pronounced kjfk/exports)
      {:navaid-pronounced
       (:navaids-by-pronunciation kjfk/exports)})))

(defn find-command [input]
  (actual-find-command @machine input))

(defn find-instructions [input]
  (-> input
      find-command
      :instructions))

(deftest find-command-test
  (testing "Simple nop with callsign"
    (is (= {:callsign "N1"
            :instructions [[:standby]]}
           (find-command "piper one standby"))))

  (testing "General Aviation callsigns"
    (is (= "N827DJ"
           (:callsign
             (find-command "piper eight two seven delta juliet standby")))))

  (testing "Airline callsigns"
    (is (= "BAW4251"
           (:callsign
             (find-command "speed bird forty two fifty one standby"))))
    (is (= "BAW112"
           (:callsign
             (find-command "speed bird one twelve standby"))))
    (is (= "BAW1006"
           (:callsign
             (find-command "speed bird ten zero six standby")))))

   (testing "Multiple instructions"
    (is (= {:callsign "N2"
            :instructions [[:radar-contact]
                           [:adjust-altitude 11000]
                           [:contact-other :tower {:frequency nil
                                                   :pleasant? false}]]}
           (find-command "piper two radar contact climb maintain one one thousand contact tower"))))

   (testing "Handle unparseable input gracefully"
     (with-out-str ; NOTE: The (expected) failing parse below normally prints
       (let [instructions (find-instructions
                            "piper one fly heading two three four have fun")]
         (is (= [[:steer 234]]
                (pop instructions)))
         (is (= :error (-> instructions last first))))))

   (testing "Ignore everything before a disregard"
     ; When we *end* with disregard... ignore the entire thing!
     (is (nil?
           (find-command "piper one proceed direct disregard")))

     (is (= {:callsign "N2"
             :instructions [[:direct "JFK"]]}
            (find-command "piper one proceed direct disregard piper two proceed disregard piper two proceed direct kennedy")))))

(deftest navaid-test
  (testing "Pronounceable navaids"
    (is (= [[:direct "MERIT"]]
           (find-instructions
             "piper one proceed direct merit")))
    (is (= [[:direct "LGA"]]
           (find-instructions
             "piper one proceed direct la guardia"))))

  (testing "Spelled navaids"
    (is (= [[:direct "BDR"]]
           (find-instructions
             "piper one proceed direct bravo delta romeo v o r")
           (find-instructions
             "piper one proceed direct bravo delta romeo")))
     (is (= [[:direct "TOWIN"]]
           (find-instructions
             "piper one proceed direct tango oscar whiskey india november")
           (find-instructions
             "piper one proceed direct tango oscar whiskey india november intersection")))))

(deftest expect-runway-test
  (testing "Process expect runway"
    (is (= [[:expect-runway "22L" {:approach-type :ils}]]
           (find-instructions
             "piper one expect i l s runway two two left")))
    (is (= [[:expect-runway "22L" {:approach-type nil}]]
           (find-instructions
             "piper one expect runway two two left")))))

(deftest steer-test
  (testing "Process steer direction"
    (is (= [[:steer 985 :right]]
           (find-instructions
             "piper one turn right heading niner eight fife")))))

(deftest cleared-approach-test
  (testing "Clear ILS approach to runway"
    (is (= [[:cleared-approach :ils "30L"]]
           (find-instructions
             "piper one cleared i l s runway tree zero left approach")))
    (is (= [[:cleared-approach :ils "22"]]
           (find-instructions
             "piper one cleared i l s runway two two approach"))))
   (testing "Clear visual approach to runway"
    (is (= [[:cleared-approach :visual "30L"]]
           (find-instructions
             "piper one cleared visual approach runway tree zero left")))))

(deftest verify-atis-test
  (testing "Request to verify ATIS"
    (is (= [[:verify-atis "A"]]
           (find-instructions
             "piper one verify you have information alpha")))
    (is (= [[:verify-atis "A"]]
           (find-instructions
             "piper one information alpha is current")))))
