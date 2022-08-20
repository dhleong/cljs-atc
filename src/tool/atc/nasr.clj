(ns atc.nasr
  (:require
   [atc.nasr.apt :as apt]
   [atc.nasr.fix :as fix]
   [atc.nasr.stardp :as stardp]
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

(defn find-fixes [zip-file & query]
  (with-open [in (okay/open-zip-file zip-file "FIX.txt")]
    (apply fix/find-fixes in query)))

(defn find-procedures [zip-file airport-id]
  (with-open [in (okay/open-zip-file zip-file "STARDP.txt")]
    (stardp/find-procedures in airport-id)))
