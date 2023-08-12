(ns atc.nasr.twr
  (:require
   [atc.nasr.types :refer [justified-keyword]]
   [atc.okay :as okay :refer [compile-record ignore-bytes justified-string]]
   [clojure.string :as str]))

(def twr3-record
  (compile-record
    [:frequencies (okay/repeated-record
                    9
                    (compile-record
                      [:frequency (justified-string 44)]
                      [:usage (justified-string 50)]))]
    [::ignore (ignore-bytes 756)]))

(def twr-file-record
  ((okay/with-bytes-count 1610)
   (compile-record
     [:type (justified-keyword 4)]
     [:id (justified-string 4)]
     (okay/record-by-type :type {:twr3 twr3-record}))))

(defn- extract-sectorization [{:keys [frequency usage]}]
  (let [[frequency sectorization] (str/split frequency #" ;")]
    {:frequency frequency
     :sectorization sectorization
     :usage usage}))

(defn find-terminal-frequencies [in id]
  (->> (okay/fixed-record-sequence twr-file-record in)
       (into
         []
         (comp
           (drop-while #(not= (:id %) id))
           (take-while #(= (:id %) id))
           (mapcat :frequencies)
           (remove (comp empty? :frequency))
           (map extract-sectorization)))))

(comment
  #_:clj-kondo/ignore
  (try
    (with-open [in (clojure.java.io/input-stream
                     (clojure.java.io/file
                       "nasr-data/TWR.txt"))]
      (time
        (let [data (find-terminal-frequencies in "JFK")]
          (clojure.pprint/pprint data))))
    (catch Throwable e
      (prn (type e)
           (ex-message e)
           (ex-data e))
      (prn (ex-message (ex-cause e)))
      (.printStackTrace e))))
