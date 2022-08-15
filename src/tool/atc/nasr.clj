(ns atc.nasr
  (:require
   [clojure.string :as str])
  (:import (java.nio.charset Charset)
           (okio Buffer Okio)))

(defn- justified-string
  ([bytes-length] (justified-string bytes-length nil))
  ([bytes-length charset]
   (let [charset-obj (if (some? charset)
                       (Charset/forName charset)
                       (Charset/defaultCharset))]
     #(str/trim (.readString % bytes-length charset-obj)))))

(defn- justified-keyword [bytes-length]
  (comp
    keyword
    #(str/replace % #" " "-")
    str/lower-case
    (justified-string bytes-length)))

(defn- ignore-bytes [bytes-count]
  (fn
    ([^Buffer frame]
     (.skip frame bytes-count)
     ::ignored-value)
    ([output ^Buffer frame]
     (ignore-bytes frame)
     output)))

(defn- optional-string [parser]
  (fn optional [v]
    (when-not (str/blank? v)
      (parser v))))

(defn- justified-string-number [str->number bytes-length & {:keys [optional?]}]
  (let [f #(try (str->number %)
                (catch Throwable cause
                  (throw (ex-info (str "Failed to parse `" % "` into a number")
                                  {:value %}
                                  cause))))]
    (comp
      (if optional?
        (optional-string f)
        f)
      (justified-string bytes-length))))

(def ^:private justified-int (partial justified-string-number #(Long/parseLong %)))
(def ^:private justified-float (partial justified-string-number #(Double/parseDouble %)))

(def ^:private formatted-coordinate
  (comp
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

(defn- compile-record-part [part]
  (cond
    (vector? part) (let [[k reader] part]
                     (fn [output frame]
                       (try
                         (let [read-value (reader frame)]
                           (if-not (= ::ignored-value read-value)
                             (assoc output k read-value)
                             output))
                         (catch Throwable cause
                           (throw (ex-info (str "Failed to read " k)
                                           {:frame frame
                                            :output output}
                                           cause))))))

    ; TODO wrap with exception handler
    (fn? part) part

    :else (throw (ex-info (str "Unexpected record part:" part) {:part part}))))

(defn- compile-record [& parts]
  (map compile-record-part parts))

(def apt-record
  (compile-record
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
    [::geographic-data (ignore-bytes 114)]  ; TODO magnetic variation, probably

    [:boundary-artcc-id (justified-string 4)]

    [::misc (ignore-bytes 569)]

    [:icao (justified-string 7)]))

(def rwy-record
  (compile-record
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

(defn read-record
  ([record frame] (read-record record frame {}))
  ([record frame initial-output]
   (reduce
     (fn [output record-part]
       (record-part output frame))
     initial-output
     record)))


(def record-type->record
  {:apt apt-record
   :rwy rwy-record})

(def apt-file-record
  [(compile-record-part [:type (justified-keyword 3)])
   (fn [output frame]
     (if-let [record (get record-type->record (:type output))]
       (read-record record frame output)

       ; otherwise, ignore
       (assoc output ::ignored? true)))])

(defn- read-sequence [in record]
  (let [source (-> (Okio/source in) (Okio/buffer))
        frame (Buffer.)
        frame-length 1533
        read-next (fn read-next []
                    (.clear frame)
                    (.readFully source frame frame-length)
                    (read-record record frame))
        read-seq (fn read-seq []
                   (cons (read-next)
                         (lazy-seq (read-seq))))]
    (read-seq)))

(defn find-airport-data [in expected-icao]
  (loop [all-records (read-sequence in apt-file-record)
         found? false
         result {}]
    (let [current (first all-records)
          did-find? (and (= :apt (:type current))
                         (= expected-icao (:icao current)))]
      (cond
        (not found?)
        (recur (next all-records)
               (or found? did-find?)
               (if did-find?
                 (assoc result :apt current)
                 result))

        ; new apt record
        (and found? (= :apt (:type current)))
        result

        :else
        (recur
          (next all-records)
          true
          (update result (:type current) conj current))))))

#_(defn scan-for [in expected-icao]
  (let [source (-> (Okio/source in) (Okio/buffer))
        frame (Buffer.)
        frame-length 1533
        charset (Charset/forName "ascii")]
    (loop []
      (.clear frame)
      (.readFully source frame frame-length)

      (let [icao-buffer (doto (.copy frame)
                            (.skip 1210))
              icao (str/trim (.readString icao-buffer 7 charset))]
          (if (= icao expected-icao)
            (read-record apt-record frame)
            (recur))))))

(comment
  #_:clj-kondo/ignore
  (try
    (with-open [in (clojure.java.io/input-stream "/Users/daniel/Downloads/28DaySubscription_Effective_2022-07-14/APT.txt")]
      (time
        (let [data (find-airport-data in "KJFK")]
          (println (dissoc data :rmk)))))
    (catch Throwable e
      (prn (ex-message e)
           (ex-data e))
      (prn (ex-message (ex-cause e)))
      #_(.printStackTrace e))))
