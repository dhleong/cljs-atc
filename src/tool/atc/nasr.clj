(ns atc.nasr
  (:require
   [atc.nasr.aff :as aff]
   [atc.nasr.apt :as apt]
   [atc.nasr.arb :as arb]
   [atc.nasr.cdr :as cdr]
   [atc.nasr.fix :as fix]
   [atc.nasr.nav :as nav]
   [atc.nasr.stardp :as stardp]
   [atc.nasr.twr :as twr]
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

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn find-artcc-boundaries [zip-file artcc-id]
  (with-open [in (okay/open-zip-file zip-file "ARB.txt")]
    (arb/find-artcc-boundaries in artcc-id)))

(defn find-arrival-routes [zip-file icao]
  (with-open [in (okay/open-zip-file zip-file "CDR.txt")]
    (cdr/find-arrival-routes in icao)))

(defn find-departure-routes [zip-file icao]
  (with-open [in (okay/open-zip-file zip-file "CDR.txt")]
    (cdr/find-departure-routes in icao)))

(defn find-facilities
  ([zip-file] (find-facilities zip-file nil))
  ([zip-file artcc-id]
   (with-open [in (okay/open-zip-file zip-file "AFF.txt")]
     (aff/find-facilities in artcc-id))))

(defn find-fixes [zip-file & query]
  (with-open [in (okay/open-zip-file zip-file "FIX.txt")]
    (apply fix/find-fixes in query)))

(defn find-navaids [zip-file & query]
  (with-open [in (okay/open-zip-file zip-file "NAV.txt")]
    (apply nav/find-navaids in query)))

(defn find-procedures [zip-file airport-id]
  (with-open [in (okay/open-zip-file zip-file "STARDP.txt")]
    (stardp/find-procedures in airport-id)))

(defn find-terminal-frequencies [zip-file facility-id]
  (with-open [in (okay/open-zip-file zip-file "TWR.txt")]
    (twr/find-terminal-frequencies in facility-id)))
