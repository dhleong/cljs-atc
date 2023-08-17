(ns atc.kmz
  (:require
   [atc.okay :as okay]
   [clojure.xml :as xml]
   [clojure.string :as str]))

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
    (let [kmz-file (clojure.java.io/file
                     (System/getenv "HOME")
                     "Downloads/2020ADS-BAirspaceMap.kmz")]
      (clojure.pprint/pprint
        (doall
          (find-airspace-regions-in-kmz kmz-file "NEW YORK"))))))
