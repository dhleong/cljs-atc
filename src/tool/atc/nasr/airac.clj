(ns atc.nasr.airac
  (:import
    (java.text SimpleDateFormat)
   (java.util Calendar)))

(def ^:private nasr-2017-start (doto (Calendar/getInstance)
                                 (.set 2017 Calendar/MARCH 2)))

(def ^:private nasr-2017-period-days 28)

(def ^:private day-in-millis (* 24 3600 1000))

(defn airac-data
  ([] (airac-data (Calendar/getInstance)))
  ([now]
   (let [nasr-start (.getTimeInMillis nasr-2017-start)
         nasr-delta (- (.getTimeInMillis now)
                       nasr-start)
         nasr-days (/ nasr-delta day-in-millis)
         nasr-periods (int (/ nasr-days nasr-2017-period-days))

         period-start (-> (doto (Calendar/getInstance)
                            (.setTimeInMillis nasr-start)
                            (.add Calendar/DAY_OF_YEAR (* nasr-periods nasr-2017-period-days)))
                          (.getTime))
         url-date-formatted (-> (SimpleDateFormat. "yyyy-MM-dd")
                                (.format period-start))
         zip-name (str "28DaySubscription_Effective_" url-date-formatted ".zip")]
     {:period-start period-start
      :zip-name zip-name
      :url (str "https://nfdc.faa.gov/webContent/28DaySub/" zip-name)})))
