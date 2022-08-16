(ns atc.nasr
  (:require
   [atc.nasr.apt :as apt]
   [atc.okay :as okay]
   [clojure.java.io :as io]))

(defn locate-zip [airac destination-directory]
  (let [expected-file (io/file destination-directory (:zip-name airac))]
    (if (.exists expected-file)
      expected-file

      (do
        (println "Downloading " (:url airac) "...")
        (time
          (with-open [in (io/input-stream (:url airac))
                      out (io/output-stream expected-file)]
            (io/copy in out)))
        expected-file))))

(defn find-airport-data [zip-file expected-icao]
  (with-open [in (okay/open-zip-file zip-file "APT.txt")]
    (apt/find-airport-data in expected-icao)))

