(ns atc.nasr.fix
  (:require
   [atc.nasr.types :refer [create-formatted-coordinate justified-keyword]]
   [atc.okay :as okay :refer [compile-record compile-record-part ignore-bytes
                              justified-string read-record]]))

(def ^:private formatted-coordinate (create-formatted-coordinate 14))

(def fix1-record
  ((okay/with-bytes-count 462)
   (compile-record
     [:id (justified-string 30)]
     [:state-name (justified-string 30)]
     [:icao-region-code (justified-string 2)]
     [:latitude formatted-coordinate]
     [:longitude formatted-coordinate]

     [::ignored (ignore-bytes 139)]

     [:artcc-high (justified-string 4)]
     [:artcc-low (justified-string 4)])))

(def record-type->record
  {:fix1 fix1-record})

(def fix-file-record
  ((okay/with-bytes-count 466)
   (compile-record
     (compile-record-part [:type (justified-keyword 4)])
     (fn [output frame]
       (if-let [record (get record-type->record (:type output))]
         (read-record record frame output)

         ; otherwise, ignore
         (assoc output ::ignored? true))))))

(defn find-fixes-for-artcc [in artcc]
  (->> (okay/fixed-record-sequence fix-file-record in)
       (transduce
         (comp
           (filter #(= :fix1 (:type %)))
           (filter #(or (= artcc (:artcc-high %))
                        (= artcc (:artcc-low %)))))
         conj [])
       (filter #(= :fix1 (:type %)))))

(comment
  #_:clj-kondo/ignore
  (try
    (with-open [in (clojure.java.io/input-stream
                     (clojure.java.io/file
                       (System/getenv "HOME")
                       "Downloads/28DaySubscription_Effective_2022-07-14/FIX.txt"))]
      (time
        (let [data (find-fixes-for-artcc in "ZNY")]
          (clojure.pprint/pprint data))))
    (catch Throwable e
      (prn e)
      (prn (type e)
           (ex-message e)
           (ex-data e))
      (prn (ex-message (ex-cause e)))
      #_(.printStackTrace e))))
