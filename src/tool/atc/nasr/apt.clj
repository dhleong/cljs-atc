(ns atc.nasr.apt
  (:require
   [atc.okay :as okay :refer [compile-record compile-record-part
                              ignore-bytes justified-float
                              justified-int justified-string optional-string read-record]]
   [clojure.string :as str]))

(defn- justified-keyword [bytes-length]
  (okay/compose
    keyword
    #(str/replace % #" " "-")
    str/lower-case
    (justified-string bytes-length)))

(def ^:private formatted-coordinate
  (okay/compose
    (optional-string
      (fn [formatted-s]
        (let [[degrees minutes seconds-and-declination] (str/split formatted-s #"-")]
          (try
            (keyword
              (str
                (last seconds-and-declination)
                degrees "*"
                minutes "'"
                (subs seconds-and-declination 0 (dec (count seconds-and-declination)))))
            (catch Exception e
              (throw (ex-info (str "Failed to parse coordinate: `" formatted-s "`: " e)
                              {:input formatted-s
                               :cause e})))))))
    (justified-string 15)))

(defn- compile-apt-file-record [& parts]
  ((okay/with-bytes-count 1530)
   (apply compile-record parts)))

(def apt-record
  (compile-apt-file-record
    [:site-number (justified-string 11)]
    [:facility-type (justified-keyword 13)]
    [:id (justified-keyword 4)]
    [:effective-date (justified-string 10)]

    [:faa-region-code (justified-string 3)]
    [:faa-district-code (justified-string 4)]
    [:associated-state-code (justified-string 2)]
    [:associated-state-name (justified-string 20)]
    [:associated-county-name (justified-string 21)]
    [:associated-county-state-code (justified-string 2)]
    [:associated-city-name (justified-string 40)]
    [:name (justified-string 50)]

    [::ownership-data (ignore-bytes 340)]
    ; [::geo-data (ignore-bytes 114)]

    [:latitude formatted-coordinate]
    [::lat (ignore-bytes 12)]
    [:longitude formatted-coordinate]
    [::lon (ignore-bytes 12)]
    [::ref-point-method (ignore-bytes 1)]

    [:elevation (justified-float 7)]
    [::elevation-method (ignore-bytes 1)]
    [:magnetic-variation (justified-string 3)]

    [::remaining-geographic-data (ignore-bytes 48)]

    [:boundary-artcc-id (justified-string 4)]

    [::misc (ignore-bytes 569)]

    [:icao (justified-string 7)]))

(def rwy-record
  (compile-apt-file-record
    [:site-number (justified-string 11)]
    [:state-code (justified-string 2)]
    [:id (justified-string 7)]
    [:length (justified-int 5)]
    [:width (justified-int 4)]

    [::physical-data (ignore-bytes 33)]

    [:base-end-id (justified-string 3)]
    [::base-end-misc (ignore-bytes 20)]

    [:base-end-latitude formatted-coordinate]
    [::base-end-lat (ignore-bytes 12)]

    [:base-end-longitude formatted-coordinate]
    [::base-end-longitude (ignore-bytes 12)]

    [:base-end-elevation (justified-float 7 :optional? true)]

    [::base-end-misc (ignore-bytes 79)]
    [::base-end-lighting (ignore-bytes 20)]
    [::base-end-object (ignore-bytes 39)]

    [:reciprocal-end-id (justified-string 3)]
    [::reciprocal-end-misc (ignore-bytes 20)]

    [:reciprocal-end-latitude formatted-coordinate]
    [::reciprocal-end-lat (ignore-bytes 12)]

    [:reciprocal-end-longitude formatted-coordinate]
    [::reciprocal-end-longitude (ignore-bytes 12)]

    [:reciprocal-end-elevation (justified-float 7 :optional? true)]

    [::reciprocal-end-misc (ignore-bytes 79)]
    [::reciprocal-end-lighting (ignore-bytes 20)]
    [::reciprocal-end-object (ignore-bytes 39)]))

(def record-type->record
  {:apt apt-record
   :rwy rwy-record})

(def apt-file-record
  ((okay/with-bytes-count 1533)
   (compile-record
     (compile-record-part [:type (justified-keyword 3)])
     (fn [output frame]
       (if-let [record (get record-type->record (:type output))]
         (read-record record frame output)

         ; otherwise, ignore
         (assoc output ::ignored? true))))))

(def apt-icao-record
  ((okay/with-bytes-count 1533)
   (apply compile-record
     (cons
       [:type (justified-keyword 3)]
       apt-record))))

(defn find-airport-data [in expected-icao]
  (when-let [subsequent-frames (okay/search-for-fixed-record
                                 in apt-icao-record
                                 :icao (partial = expected-icao))]
    (loop [all-records (map (partial okay/read-record apt-file-record) subsequent-frames)
           result {}]
      (let [current (first all-records)]
        (if (and (= :apt (:type current))
                 (not= expected-icao (:icao current)))
          result

          (recur
            (next all-records)
            (update result (:type current) conj current)))))))

(comment
  #_:clj-kondo/ignore
  (try
    (with-open [in (clojure.java.io/input-stream
                     (clojure.java.io/file
                       (System/getenv "HOME")
                       "Downloads/28DaySubscription_Effective_2022-07-14/APT.txt"))]
      (time
        (let [data (find-airport-data in "KJFK")]
          (clojure.pprint/pprint (dissoc data :rmk)))))
    (catch Throwable e
      (prn e)
      (prn (type e)
           (ex-message e)
           (ex-data e))
      (prn (ex-message (ex-cause e)))
      #_(.printStackTrace e))))
