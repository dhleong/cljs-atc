(ns atc.nasr.fix
  (:require
   [atc.data.core :refer [coord-distance]]
   [atc.nasr.types :refer [create-formatted-coordinate justified-keyword]]
   [atc.okay :as okay :refer [compile-record compile-record-part ignore-bytes
                              justified-string read-record]]))

(def ^:private formatted-coordinate (create-formatted-coordinate 14))

(def fix1-record
  (compile-record
    [:id (justified-string 30)]
    [:state-name (justified-string 30)]
    [:icao-region-code (justified-string 2)]
    [:latitude formatted-coordinate]
    [:longitude formatted-coordinate]

    [::ignored (ignore-bytes 139)]

    [:artcc-high (justified-string 4)]
    [:artcc-low (justified-string 4)]))

(def fix5-record
  (compile-record
    [:id (justified-string 30)]
    [:state-name (justified-string 30)]
    [:icao-region-code (justified-string 2)]
    [:chart-type (justified-keyword 22)]))

(def record-type->record
  {:fix1 fix1-record
   :fix5 fix5-record})

(def fix-file-record
  ((okay/with-bytes-count 468)
   (compile-record
     (compile-record-part [:type (justified-keyword 4)])
     (fn [output frame]
       (if-let [record (get record-type->record (:type output))]
         (read-record record frame output)

         ; otherwise, ignore
         (assoc output ::ignored? true))))))

(defn- conj-kws-into-set [a b]
  (cond
    ; non-keywords are just replaced
    (not (keyword? b)) b

    ; If a is already a set, just conj b onto it
    (set? a) (conj a b)

    ; Otherwise, make a new set
    :else #{a b}))

(defn find-fixes-xf [in pred]
  (->> (okay/fixed-record-sequence fix-file-record in)
       (transduce
         (comp
           (filter #(contains? #{:fix1 :fix5} (:type %)))

           ; Merge related records together
           (partition-by :id)
           (map #(apply merge-with conj-kws-into-set %))

           pred)
         conj [])))

(defn find-fixes [in & {:keys [near in-range in-artcc on-charts]
                        :or {in-range (* 10 1000)}}]
  (let [predicates (keep
                     identity
                     [(when near
                        (comp
                          (map #(let [fix-coord [(:latitude %) (:longitude %)]]
                                  (assoc % :distance-to-coord (coord-distance near fix-coord))))
                          (filter #(<= (:distance-to-coord %) in-range))))

                      (when in-artcc
                        (filter #(= in-artcc (:artcc-high %))))

                      (when on-charts
                        (filter #(some (:chart-type %) on-charts)))])]
    (find-fixes-xf in (apply comp predicates))))

(defn find-fixes-near [in coord range-m]
  (find-fixes in {:near coord :in-range range-m}))

(comment
  #_:clj-kondo/ignore
  (try
    (with-open [in (clojure.java.io/input-stream
                     (clojure.java.io/file
                       (System/getenv "HOME")
                       "Downloads/28DaySubscription_Effective_2022-07-14/FIX.txt"))]
      (let [data (time (find-fixes-near in [:N40.63992778 :W73.77869167 13]
                                        (* 10 1000)))
            names #_(->> data (map :id) sort)
            (->> data (group-by :artcc-high) keys)]
        (println names)
        (println "Found" (count data) "points")))
    (catch Throwable e
      (prn (type e)
           (ex-message e)
           (ex-data e))
      (prn (ex-message (ex-cause e)))
      (.printStackTrace e)))

  #_:clj-kondo/ignore
  (try
    (with-open [in (clojure.java.io/input-stream
                     (clojure.java.io/file
                       (System/getenv "HOME")
                       "Downloads/28DaySubscription_Effective_2022-07-14/FIX.txt"))]
      (let [data (time (find-fixes
                         in
                         ; :in-artcc "ZNY"
                         :near [:N40.63992778 :W73.77869167 13]
                         :in-range (* 100 1000)
                         :on-charts #{:sid :star}))]
        (prn (->> (map :id data) sort))
        ; (prn (->> data (filter #(= "MERIT" (:id %))) first))
        (println "for query" (count data)))
      #_(let [data (time (->> (okay/fixed-record-sequence fix-file-record in)
                            (filter #(= "MERIT" (:id %)))
                            first))]
        (prn data)
        (println (coord-distance
                   [:N40.63992778 :W73.77869167 13]
                   [(:latitude data) (:longitude data)]))))
    (catch Throwable e
      (prn (type e)
           (ex-message e)
           (ex-data e))
      (prn (ex-message (ex-cause e)))
      (.printStackTrace e))))
