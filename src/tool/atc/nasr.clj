(ns atc.nasr
  (:require
   [clojure.string :as str]
   [gloss.core :as g :refer [defcodec header]]
   [gloss.data.bytes.core :refer [drop-bytes take-contiguous-bytes]]
   [gloss.io :refer [decode lazy-decode-all]]))

(defn- left-justified [frame]
  (g/compile-frame
    frame
    identity
    str/trimr))

(defn- left-justified-string [length]
  (left-justified
    (g/string :utf-8 :length length)))

(defn- left-justified-keyword [length]
  (g/compile-frame
    (left-justified-string length)
    (comp str/upper-case name)
    (comp keyword #(str/replace % #"[ ]" "-") str/lower-case)))

(defn- ignored-chars [chars-count]
  (g/string :utf-8 :length chars-count :char-sequence true))

(defn- right-justified-number [parse chars-count]
  (g/compile-frame
    (g/string :ascii :length chars-count)
    identity
    (comp
      #(if (str/blank? %)
         nil
         (parse %))
      str/triml)))

(def ^:private right-justified-int (partial right-justified-number #(Long/parseLong %)))
(def ^:private right-justified-float (partial right-justified-number #(Double/parseDouble %)))

(def ^:private formatted-coordinate
  (g/compile-frame
    (left-justified-string 15)
    identity
    (fn [formatted-s]
      (delay
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
                               :cause e})))))))))

(defcodec apt-file-record-type
  (left-justified-keyword 3))

(defn apt-file-frame [content]
  ; NOTE: The record is 1531 bytes + \r\n, but that includes the 3-byte "type" header
  (g/finite-frame 1530 content))

(defcodec apt-record
  (apt-file-frame
    (g/ordered-map
      :type :apt
      :site-number (left-justified-string 11)
      :facility-type (left-justified-keyword 13)
      :id (left-justified-keyword 4)
      :effective-date (left-justified-string 10)

      :faa-region-code (left-justified-string 3)
      :faa-district-code (left-justified-string 4)
      :associated-state-code (left-justified-string 2)
      :associated-state-name (left-justified-string 20)
      :associated-county-name (left-justified-string 21)
      :associated-county-state-code (left-justified-string 2)
      :associated-city-name (left-justified-string 40)
      :name (left-justified-string 50)

      :ownership-data (ignored-chars 340)
      :geographic-data (ignored-chars 114) ; TODO magnetic variation, probably

      :boundary-artcc-id (left-justified-string 4)

      ::rest (g/string :ascii)
      )))

(defcodec rwy-record
  (apt-file-frame
    (g/ordered-map
      :type :rwy
      :site-number (left-justified-string 11)
      :state-code (left-justified-string 2)
      :id (left-justified-string 7)
      :length (right-justified-int 5)
      :width (right-justified-int 4)
      ::physical-data (ignored-chars 33)

      :base-end-id (left-justified-string 3)
      ::base-end-misc (ignored-chars 20)

      :base-end-latitude formatted-coordinate
      ::base-end-lat (ignored-chars 12)

      :base-end-longitude formatted-coordinate
      ::base-end-longitude (ignored-chars 12)

      :base-end-elevation (right-justified-float 7)

      ::base-end-misc (ignored-chars 79)
      ::base-end-lighting (ignored-chars 20)
      ::base-end-object (ignored-chars 39)

      :reciprocal-end-id (left-justified-string 3)
      ::reciprocal-end-misc (ignored-chars 20)

      :reciprocal-end-latitude formatted-coordinate
      ::reciprocal-end-lat (ignored-chars 12)

      :reciprocal-end-longitude formatted-coordinate
      ::reciprocal-end-longitude (ignored-chars 12)

      :reciprocal-end-elevation (right-justified-float 7)

      ::reciprocal-end-misc (ignored-chars 79)
      ::reciprocal-end-lighting (ignored-chars 20)
      ::reciprocal-end-object (ignored-chars 39)

      ::rest (g/string :ascii))))

(defn ignored [type-kw]
  (apt-file-frame
    (g/ordered-map
      :type type-kw
      ::rest (g/string :ascii))))

(def record-type->codec
  {:apt apt-record
   :rwy rwy-record})

(defcodec apt-file-record
  (header
    apt-file-record-type
    (fn [record-type]
      (get record-type->codec record-type (ignored record-type)))
    :type))

(defcodec apt-file-raw-record
  (g/finite-frame
    1533
    (g/ordered-map
      :type apt-file-record-type
      :data (g/finite-block 1530))))

(defn scan-for [reader expected-icao]
  (let [all-records (lazy-decode-all apt-file-raw-record reader)
        skipped (->> all-records
                     #_(take 500)
                     (drop-while #(not (and (= :apt (:type %))

                                            (let [icao-buffer (-> (:data %)
                                                                  (drop-bytes 1207)
                                                                  (take-contiguous-bytes 7))
                                                  icao (str/trim
                                                         (str (char (.get icao-buffer))
                                                              (char (.get icao-buffer))
                                                              (char (.get icao-buffer))
                                                              (char (.get icao-buffer))
                                                              (char (.get icao-buffer))
                                                              (char (.get icao-buffer))
                                                              (char (.get icao-buffer))))]
                                              (= expected-icao icao))))))]
    (println (decode apt-record (:data (first skipped))))
    (println (first skipped))))

(comment
  #_:clj-kondo/ignore
  (with-open [reader (clojure.java.io/reader "/Users/daniel/Downloads/28DaySubscription_Effective_2022-07-14/APT.txt")]
    (time
      (scan-for reader "KJFK")
      #_(let [site-number (atom nil)
            entries (->> (lazy-decode-all apt-file-record reader)
                         (transduce
                           (comp
                             (drop-while #(not= "JFK" (:id %)))
                             (take-while #(if (nil? @site-number)
                                            (reset! site-number (:site-number %))
                                            (not= :apt (:type %)))))
                           conj [])
                         #_(group-by :site-number)
                         #_(filter #(= (:type %) :apt))
                         #_(filter #(= (:type %) :rwy)))]
        #_(doseq [a entries]
            (prn "At:" (:type a) (:site-number a) (:id a) (:associated-city-name a) (:name a)))
        (prn (first entries))))
    )
  )
