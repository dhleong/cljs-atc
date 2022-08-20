(ns atc.nasr.cdr
  (:require
   [atc.okay :as okay]
   [clojure.string :as str]))

(defn find-departure-routes [in icao]
  (->> in
       (okay/newlines-sequence)
       (into
         []
         (comp
           (map #(str/split % #","))
           (map (fn [[route-code origin destination departure-fix route-string artcc]]
                  {:route-code route-code
                   :origin origin
                   :destination destination
                   :departure-fix departure-fix
                   :route-string route-string
                   :artcc artcc}))
           (filter #(= icao (:origin %)))))))

(comment
  #_:clj-kondo/ignore
  (try
    (with-open [in (clojure.java.io/input-stream
                     (clojure.java.io/file
                       (System/getenv "HOME")
                       "Downloads/28DaySubscription_Effective_2022-07-14/CDR.txt"))]
      (let [data (time (find-departure-routes
                         in
                         "KJFK"))
            found-count (count data)]
        (clojure.pprint/pprint (->> data
                                    (map :departure-fix)
                                    (into #{})))

        (println "for query" found-count)))
    (catch Throwable e
      (prn (type e)
           (ex-message e)
           (ex-data e))
      (prn (ex-message (ex-cause e)))
      (.printStackTrace e))))
