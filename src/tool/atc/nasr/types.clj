(ns atc.nasr.types
  (:require
    [atc.okay :as okay :refer [justified-string optional-string]]
    [clojure.string :as str]))

(defn justified-keyword [bytes-length]
  (okay/compose
    keyword
    #(str/replace % #" " "-")
    str/lower-case
    (justified-string bytes-length)))

(defn create-formatted-coordinate [bytes-length]
  (okay/compose
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
    (justified-string bytes-length)))
