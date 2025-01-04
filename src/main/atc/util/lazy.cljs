(ns atc.util.lazy
  (:require-macros [atc.util.lazy])
  (:require
   [promesa.core :as p]
   [shadow.lazy :as lazy]))

(defn unpack [lazy-loadable]
  (if (lazy/ready? lazy-loadable)
    (p/do! @lazy-loadable)

    ; NOTE: lazy/load *should* return a promise, but it
    ; does not seem to play well with promesa, so...
    (p/create
     (fn [p-resolve p-reject]
       (lazy/load lazy-loadable p-resolve p-reject)))))

(defn function [lazy-loadable]
  (fn lazy-wrapper [& args]
    (p/let [f (unpack lazy-loadable)]
      (apply f args))))
