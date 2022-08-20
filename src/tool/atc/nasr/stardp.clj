(ns atc.nasr.stardp
  (:require
   [atc.nasr.types :refer [justified-keyword]]
   [atc.okay :as okay :refer [compile-record ignore-bytes justified-string
                              optional-string]]
   [clojure.string :as str]))

(defn- stardp-coordinate [degrees-length]
  (okay/compose
    (optional-string
      (fn [formatted]
        (let [degrees-end (+ 1 degrees-length)
              minutes-end (+ 2 degrees-end)
              seconds-end (+ 2 minutes-end)

              declination (first formatted)
              degrees (subs formatted 1 degrees-end)
              minutes (subs formatted degrees-end minutes-end)
              seconds (subs formatted minutes-end seconds-end)
              tenths-of-seconds (subs formatted seconds-end)]
          (try
            (keyword
              (str
                declination
                degrees "*"
                minutes "'"
                seconds "." tenths-of-seconds))
            (catch Exception e
              (throw (ex-info (str "Failed to parse coordinate: `" formatted "`: " e)
                              {:input formatted
                               :cause e})))))))
    (justified-string (+ degrees-length 6))))

(def stardp-record
  (compile-record
    [:seqno (justified-string 5)]
    (ignore-bytes 5)
    [:type (justified-keyword 2)]
    (ignore-bytes 1)

    [:latitude (stardp-coordinate 2)]
    [:longitude (stardp-coordinate 3)]
    [:fix-id (justified-string 6)]
    [:icao-region-code (justified-string 2)]
    [:computer-code (justified-string 13)]
    [:procedure-name (justified-string 110)]

    [::airaways-navids-using-numbered-fix (ignore-bytes 62)]
    [::newlines (ignore-bytes 2)]))

(defn find-procedures [in airport-id]
  (->> (okay/fixed-record-sequence stardp-record in)

       ; Merge related records together
       (partition-by :seqno)

       (transduce
         (comp
           (filter (partial some #(and (= :aa (:type %))
                                       (= airport-id (:fix-id %)))))

           ; Different parts or versions of a procedure are separated by the first
           ; record in sequence having the name of the procedure. This bit splits these
           ; parts up and combines them into a sequence of sequences

           (map (partial partition-by #(not (str/blank? (:procedure-name %)))))
           (map (partial partition-all 2))
           (mapcat (partial map (partial apply concat)))

           ; Now we clean it up so each procedure begins with its name
           (map (fn [parts]
                  (assoc (select-keys (first parts) [:computer-code :icao-region-code :procedure-name :seqno])
                         :fixes
                         (map #(dissoc % :computer-code :procedure-name :seqno :icao-region-code) parts))))

           ; There may be a better way to do this, but... split up again by :seqno and compose each
           ; procedure with its variants and transitions
           (partition-by :seqno)
           (map (fn [parts]
                  (let [procedure-name (:procedure-name (first parts))
                        is-path-variation? #(= (:procedure-name %) procedure-name)]
                    {:computer-code (:computer-code (first parts))
                     :procedure-name procedure-name

                     ; This is a bit of hacks, technically: the sequence number is subject to change,
                     ; and we *should* be detecting the change in seqno... sequence
                     :type (if (str/starts-with? (:seqno (first parts)) "S")
                             :arrival
                             :departure)

                     :paths (->> parts
                                 (filter is-path-variation?)
                                 (map :fixes))
                     :transitions (->> parts
                                       (remove is-path-variation?)
                                       (map #(select-keys % [:computer-code :procedure-name :fixes]))
                                       seq)}))))
         conj [])))

(comment
  #_:clj-kondo/ignore
  (try
    (with-open [in (clojure.java.io/input-stream
                     (clojure.java.io/file
                       (System/getenv "HOME")
                       "Downloads/28DaySubscription_Effective_2022-07-14/STARDP.txt"))]
      (let [data (time (find-procedures in "JFK"))
            found-count (count data)]
        ; (clojure.pprint/pprint (first data))
        (clojure.pprint/pprint (map (juxt :type :computer-code) data))

        (println "for query" found-count)))
    (catch Throwable e
      (prn (type e)
           (ex-message e)
           (ex-data e))
      (prn (ex-message (ex-cause e)))
      (.printStackTrace e))))
