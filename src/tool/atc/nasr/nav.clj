(ns atc.nasr.nav
  (:require
   [atc.nasr.types :refer [create-formatted-coordinate justified-keyword]]
   [atc.okay :as okay :refer [compile-record ignore-bytes justified-string]]))

(def ^:private formatted-coordinate (create-formatted-coordinate 14))

(def nav1-record
  (compile-record
    [:id (justified-string 4)]
    [:type (justified-keyword 20)]
    [:official-id (justified-string 4)]
    [:effective-date (justified-string 10)]
    [:name (justified-string 30)]
    [:city (justified-string 40)]
    [:state-name (justified-string 30)]
    [:state-code (justified-string 2)]
    [:faa-region (justified-string 3)]

    [::misc-administrative (ignore-bytes 224)]

    [:latitude formatted-coordinate]
    [::latitude-seconds (ignore-bytes 11)]

    [:longitude formatted-coordinate]
    [::longitude-seconds (ignore-bytes 11)]

    [::misc (ignore-bytes 78)]

    [:radio-voice-call (okay/compose
                         (fn [string-value]
                           (when-not (= "NONE" string-value)
                             string-value))
                         (justified-string 30))]))

(def nav-file-record
  ((okay/with-bytes-count 807)
   (compile-record
     [:type (justified-keyword 4)]
     (okay/record-by-type :type {:nav1 nav1-record}))))

(defn find-navaids-xf [in pred]
  (->> (okay/fixed-record-sequence nav-file-record in)
       (transduce
         (comp
           ; Merge related records together
           (partition-by :id)
           (map #(apply merge %))

           pred)
         conj [])))

(defn find-navaids [in & {:keys [ids]}]
  (let [predicates (keep
                     identity
                     [(when ids
                        (comp
                          ; We *probably* don't want VOR test facilities
                          (filter #(not= :vot (:type %)))

                          (filter (comp (into #{} ids) :id))
                          (take (count ids))))])]
    (find-navaids-xf in (apply comp predicates))))

(comment
  #_:clj-kondo/ignore
  (try
    (with-open [in (clojure.java.io/input-stream
                     (clojure.java.io/file
                       (System/getenv "HOME")
                       "Downloads/28DaySubscription_Effective_2022-07-14/NAV.txt"))]
      (let [data (time (find-navaids
                         in
                         :ids #{"COL"}))
            found-count (count data)]
        (clojure.pprint/pprint
          (cond
            (= 1 found-count) (first data)
            (< 4 found-count) data
            :else (->> (map :id data) sort)))

        (println "for query" found-count)))
    (catch Throwable e
      (prn (type e)
           (ex-message e)
           (ex-data e))
      (prn (ex-message (ex-cause e)))
      (.printStackTrace e))))
