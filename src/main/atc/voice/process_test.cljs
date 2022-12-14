(ns atc.voice.process-test
  (:require
   [atc.data.airports.kjfk :as kjfk]
   [atc.voice.parsing.airport :as airport-parsing]
   [atc.voice.process :rename {find-command actual-find-command} :refer [build-machine]]
   [cljs.test :refer-macros [deftest is testing]]))

(def machine
  (delay
    (build-machine (airport-parsing/generate-parsing-context kjfk/airport))))

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
             (find-command "speed bird one twelve standby")))))

   (testing "Multiple instructions"
    (is (= {:callsign "N2"
            :instructions [[:adjust-altitude 11000]
                           [:contact-other :tower]]}
           (find-command "piper two climb maintain one one thousand contact tower"))))

   (testing "Handle unparseable input gracefully"
     (with-out-str ; NOTE: The (expected) failing parse below normally prints
       (let [instructions (find-instructions
                            "piper one fly heading two three four have fun")]
         (is (= [[:steer 234]]
                (pop instructions)))
         (is (= :error (-> instructions last first)))))))

(deftest navaid-test
  (testing "Pronounceable navaids"
    (is (= [[:direct "MERIT"]]
           (find-instructions
             "piper one proceed direct merit"))))

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
