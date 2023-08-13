(ns atc.game.traffic.model
  (:require
   [clojure.spec.alpha :as s]
   [spec-tools.data-spec :as ds]))

(def flight-spec
  (ds/spec
    {:name ::flight
     :spec {:aircraft {:type (s/spec #{:airline})
                       :airline string?
                       :flight-number number?
                       :destination string? ; eg KJFK
                       :runway string? ; eg 13L
                       :config map?} ; from aircraft-configs
            :delay-to-next-s number?}}))

(defprotocol ITraffic
  (next-departure [this engine]))

(defrecord ValidatedTraffic [base]
  ITraffic
  (next-departure [_this engine]
    (let [v (next-departure base engine)]
      (when-not (s/valid? flight-spec v)
        (throw (ex-info (str "Generated invalid traffic: "
                             (s/explain flight-spec v))
                        {:generated v})))
      v)))
