(ns atc.util.local-storage
  (:require
   [cognitect.transit :as t]
   [re-frame.core :refer [reg-cofx]]))

(defn load [data-key]
  (->> (js/window.localStorage.getItem (name data-key))
       (t/read (t/reader :json))))

(defn save [data-key data]
  (->> data
       (t/write (t/writer :json))
       (js/window.localStorage.setItem (name data-key))))

(reg-cofx
  ::load
  (fn [cofx data-key]
    (assoc cofx data-key (load data-key))))
