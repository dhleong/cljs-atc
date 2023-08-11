(ns atc.nasr.aff
  (:require
   [atc.nasr.types :refer [create-formatted-coordinate justified-keyword]]
   [atc.okay :as okay :refer [compile-record ignore-bytes justified-string]]))

(def ^:private formatted-coordinate (create-formatted-coordinate 14))

(def aff1-record
  (compile-record
    [:name (justified-string 40)]
    [:site-location (justified-string 30)]
    [:cross-reference (justified-string 50)]
    [:facility-type (justified-keyword 5)]
    [:effective-date (justified-string 10)]
    [:state-name (justified-string 30)]
    [:state-code (justified-string 2)]
    [:latitude formatted-coordinate]
    [::lat (ignore-bytes 11)]
    [:longitude formatted-coordinate]
    [::lng (ignore-bytes 11)]
    [::icao-id (ignore-bytes 4)]
    [::blank (ignore-bytes 25)]))

(def aff3-record
  (compile-record
    [:site-location (justified-string 30)]
    [:facility-type (justified-keyword 5)]
    [:frequency (justified-string 8)]
    [:altitude (justified-string 10)]
    [:special-usage-name (justified-string 16)]
    [:rcag? (justified-string 1)]
    [:landing-facility-location-id (justified-string 4)]
    [:state-name (justified-string 30)]
    [:state-code (justified-string 2)]
    [:city-name (justified-string 40)]
    [:airport-name (justified-string 50)]
    [::airport-location (ignore-bytes 50)]))

(def aff-file-record
  ((okay/with-bytes-count 256)
   (compile-record
     [:type (justified-keyword 4)]
     [:artcc (justified-string 4)]
     (okay/record-by-type :type {:aff1 aff1-record
                                 :aff3 aff3-record}))))

(defn compose-facility [parts]
  (let [{:keys [latitude] :as base} (first parts)
        radio (->> parts
                   (filter #(= (:altitude %) "LOW"))
                   first)]
    (when (and radio latitude)
      (merge base (select-keys radio [:frequency
                                      :altitude
                                      :landing-facility-location-id])))))

(defn find-facilities
  ([in] (find-facilities in nil))
  ([in artcc]
   (->> (okay/fixed-record-sequence aff-file-record in)
        (into
          []
          (comp
            (if artcc
              (comp
                (drop-while #(not= (:artcc %) artcc))
                (take-while #(= (:artcc %) artcc)))
              identity)
            (partition-by :site-location)
            (keep compose-facility))))))

(comment
  #_:clj-kondo/ignore
  (try
    (with-open [in (clojure.java.io/input-stream
                     (clojure.java.io/file
                       "nasr-data/AFF.txt"))]
      (time
        (let [data (find-facilities in "ZNY")]
          (clojure.pprint/pprint data))))
    (catch Throwable e
      (prn (type e)
           (ex-message e)
           (ex-data e))
      (prn (ex-message (ex-cause e)))
      (.printStackTrace e))))
