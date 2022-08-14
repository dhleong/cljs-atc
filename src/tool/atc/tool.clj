(ns atc.tool
  (:require
   [atc.xml :refer [->scanner scan-until scope tag-content=]]
   [clojure.data.xml :as xml]
   [clojure.java.io :as io]
   [clojure.string :as str]))

(defn- collect-runways [scanner airport-id]
  (loop [scanner scanner
         runways []]
    (if-let [at-runway (scan-until scanner :aixm:Runway)]
      (let [runway (scope at-runway :aixm:Runway)
            runway-id (get-in runway [:attrs :id])]
        (println runway-id)
        (if-not (str/ends-with? runway-id airport-id)
          ; We're done here
          runways

          (let [at-pos (scan-until at-runway :gml:pos)
                pos (:content (scope at-pos :gml:pos))]
            (recur
              at-runway
              (conj runways {:id runway-id
                             :designator (-> runway
                                             xml-seq
                                             ->scanner
                                             (scan-until :aixm:designator)
                                             (scope :aixm:designator)
                                             :content
                                             first)
                             :pos pos})))))

      ; No more runway tags
      runways)))

(defn build-airport [reader icao]
  (let [root (-> (xml/parse reader :namespace-aware false)
                 xml-seq
                 ->scanner)

        airport (time
                  (scan-until
                    root
                    (tag-content= :aixm:locationIndicatorICAO icao)))

        airport-id (-> airport
                       (scope :aixm:AirportHeliport)
                       (get-in [:attrs :id])
                       (subs 3) ; drop the AH_ prefix

                       ; trim leading zeroes, basically
                       Integer/parseInt
                       str)

        runways (time
                  (collect-runways airport airport-id))]

    #_:clj-kondo/ignore
    (def found airport)

    #_:clj-kondo/ignore
    (def runways runways)

    ; TODO: compose reciprocal runways

    #_(println "hi?" airport-id (count runways))
    #_(println (:tag (second (:content zipper))))
    {:id airport-id
     :runways runways})
  )

(defn -main []
  (let [path "~/Downloads/AIXM_5.1/XML-Subscriber-Files/APT_AIXM.xml"]
    (with-open [reader (io/reader path)]
      #_(println (build-airport reader "KJFK"))
      (println (build-airport reader "PFAK")))))
