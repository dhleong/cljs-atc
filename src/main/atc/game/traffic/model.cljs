(ns atc.game.traffic.model
  (:require
   [clojure.spec.alpha :as s]
   [spec-tools.data-spec :as ds]))

(def traffic-aircraft-spec
  (ds/spec
    {:name ::traffic-aircraft
     :spec {:type (s/spec #{:airline})
            :airline string?
            :flight-number number?
            :destination string? ; eg KJFK
            (ds/opt :runway) string? ; eg 13L
            :config map?}}))

(def multi-flight-spec
  (ds/spec
    {:name ::multi-flight
     :spec {:aircrafts (s/coll-of traffic-aircraft-spec)
            :delay-to-next-s number?}}))

(def flight-spec
  (ds/spec
    {:name ::flight
     :spec {:aircraft traffic-aircraft-spec
            :delay-to-next-s number?}}))

(defprotocol ITraffic
  (generate-initial-arrivals [this engine])
  (next-arrival [this engine])
  (next-departure [this engine]))

(defn- validate-generated-flight [v]
  (when-not (s/valid? flight-spec v)
    (throw (ex-info (str "Generated invalid traffic: "
                         (s/explain-str flight-spec v))
                    {:generated v})))
  v)

(defn- validate-generated-multi-flight [v]
  (when-not (s/valid? multi-flight-spec v)
    (throw (ex-info (str "Generated invalid multi-traffic: "
                         (s/explain-str multi-flight-spec v))
                    {:generated v})))
  v)


(defrecord ValidatedTraffic [base]
  ITraffic
  (generate-initial-arrivals [_this engine]
    (validate-generated-multi-flight
      (generate-initial-arrivals base engine)))
  (next-arrival [_this engine]
    (validate-generated-flight
      (next-arrival base engine)))
  (next-departure [_this engine]
    (validate-generated-flight
      (next-departure base engine))))
