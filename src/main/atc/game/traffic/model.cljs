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
  (generate-initial-arrivals [this engine])
  (next-arrival [this engine])
  (next-departure [this engine]))

(defn- validate-generated-flight [v]
  (when-not (s/valid? flight-spec v)
    (throw (ex-info (str "Generated invalid traffic: "
                         (s/explain flight-spec v))
                    {:generated v})))
  v)

(defrecord ValidatedTraffic [base]
  ITraffic
  (generate-initial-arrivals [_this engine]
    (let [arrivals (generate-initial-arrivals base engine)]
      (doseq [a arrivals]
        (validate-generated-flight a))
      arrivals))
  (next-arrival [_this engine]
    (validate-generated-flight
      (next-arrival base engine)))
  (next-departure [_this engine]
    (validate-generated-flight
      (next-departure base engine))))
