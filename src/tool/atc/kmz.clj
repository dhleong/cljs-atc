(ns atc.kmz
  (:require
   [atc.okay :as okay]
   [atc.util.with-timing :refer [with-timing]]
   [clj-http.lite.client :as http]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.xml :as xml]))

(defn locate-kmz [airac destination-directory]
  (let [expected-file (io/file destination-directory
                               (:airspace-kmz-name airac))]
    (if (.exists expected-file)
      expected-file

      (do
        (println "Downloading " (:airspace-kmz-url airac) "to" expected-file "...")
        (with-timing "Fetch kmz"
          ; NOTE: For some reason, the FAA's website 403's for Java's
          ; default user agent
          (with-open [in (-> (http/get (:airspace-kmz-url airac)
                                       {:as :stream
                                        :headers
                                        {"user-agent" "clj-atc-tool"}})
                             :body)
                      out (io/output-stream expected-file)]
            (io/copy in out)))
        expected-file))))

(defn- format-point [raw-point]
  (let [[lng lat alt] (map parse-double (str/split raw-point #","))]
    [lat lng alt]))

(defn find-airspace-regions [in airspace-name]
  (let [target-place-name (str airspace-name " CLASS B")]
    (->> (xml/parse in)
         (xml-seq)
         (sequence
           (comp
             (filter #(= :Placemark (:tag %)))
             (filter #(when-some [s (get-in % [:content 0 :content 0])]
                        (str/starts-with? s target-place-name)))
             (mapcat :content)
             (filter #(= :Polygon (:tag %)))
             (mapcat :content)
             (filter #(= :outerBoundaryIs (:tag %)))
             (map #(get-in % [:content 0 :content 0 :content 0]))
             (map str/trim)
             (map #(str/split % #"\s+"))
             (map-indexed
               (fn [idx raw-points]
                 ; TODO The <description> has the floor/ceiling, which we
                 ; could enforce
                 {:id idx
                  :points (mapv format-point raw-points)})))))))

(defn find-airspace-regions-in-kmz [kmz-file airspace-name]
  (with-open [f (-> (okay/open-zip-file
                      kmz-file
                      "doc.kml")
                    (okay/buffered-source)
                    (.inputStream))]
    (find-airspace-regions f airspace-name)))


#_:clj-kondo/ignore
(comment
  (with-open
    (let [kmz-file (io/file
                     (System/getenv "HOME")
                     "Downloads/2020ADS-BAirspaceMap.kmz")]
      (clojure.pprint/pprint
        (doall
          (find-airspace-regions-in-kmz kmz-file "NEW YORK"))))))
