(ns atc.nasr.arb
  (:require
   [atc.nasr.types :refer [create-formatted-coordinate]]
   [atc.okay :as okay :refer [compile-record ignore-bytes justified-string]]))

(def ^:private formatted-coordinate (create-formatted-coordinate 14))

(def arb-record
  ((okay/with-bytes-count 399)
   (compile-record
     [:artcc-id (justified-string 3)]
     [::structure-code (ignore-bytes 4)]
     [::point-designator (ignore-bytes 5)]
     [:center-name (justified-string 40)]
     [:altitude-structure (justified-string 10)]
     [:lat formatted-coordinate]
     [:lng formatted-coordinate]
     [:boundary-line-description (justified-string 300)]
     [::etc (ignore-bytes 9)])))

(defn find-artcc-boundaries [in artcc-id]
  (when-let [subsequent-frames (okay/search-for-fixed-record
                                 in arb-record
                                 :artcc-id (partial = artcc-id))]
    (->> subsequent-frames
         (into []
               (comp
                 (map (partial okay/read-record arb-record))
                 (take-while #(= artcc-id (:artcc-id %)))
                 (partition-by #(= "TO POINT OF BEGINNING"
                                   (:boundary-line-description %)))
                 (partition-all 2)
                 (map (partial apply concat)))))))

(comment
  #_:clj-kondo/ignore
  (try
    (with-open [in (clojure.java.io/input-stream
                     (clojure.java.io/file
                       "nasr-data/ARB.txt"))]
      (time
        (let [data (find-artcc-boundaries in "ZNY")]
          (clojure.pprint/pprint data))))
    (catch Throwable e
      (prn e)
      (prn (type e)
           (ex-message e)
           (ex-data e))
      (prn (ex-message (ex-cause e)))
      #_(.printStackTrace e))))
