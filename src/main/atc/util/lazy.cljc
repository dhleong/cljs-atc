(ns atc.util.lazy
  #? (:cljs (:require-macros [atc.util.lazy]))
  (:require
   #?(:cljs [shadow.esm :as esm])
   #?(:cljs [applied-science.js-interop :as j])
   [promesa.core :as p]
   [shadow.lazy :as lazy :refer #?(:cljs [ILoadable Loadable]
                                   :clj [])]))

#? (:clj
; Re-exporting for convenience
    #_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
    (defmacro loadable [s]
      `(lazy/loadable ~s)))

#? (:cljs
    (deftype DynamicImport [state p start-load]
      ILoadable
      (ready? [_]
        (= :ready @state))
      IDeref
      (-deref [this]
        (when-not (lazy/ready? this)
          (throw (ex-info "DynamicImport not ready yet" {})))
        (or @p
            (start-load)))))

#? (:clj
    (defn dynamic-import [s]
      (resolve s))

    :cljs
    (defn dynamic-import [s]
      (let [p (atom nil)
            state (atom nil)
            val-name (name s)
            start-load (fn []
                         (let [promise (p/let [m (esm/dynamic-import (str "./" (namespace s) ".js"))]
                                         (j/get m val-name))]
                           (reset! p promise)
                           (p/then promise #(do
                                              (reset! p %)
                                              (reset! state :ready)))
                           promise))]
        (->DynamicImport state p start-load))))

(defn unpack [lazy-loadable]
  #? (:clj (p/do! (deref lazy-loadable))
      :cljs (if (lazy/ready? lazy-loadable)
              (p/do! @lazy-loadable)

              (cond
                (instance? Loadable lazy-loadable)
                ; NOTE: lazy/load *should* return a promise, but it
                ; does not seem to play well with promesa, so...
                (p/create
                 (fn [p-resolve p-reject]
                   (lazy/load lazy-loadable p-resolve p-reject)))

                (instance? DynamicImport lazy-loadable)
                ((.-start-load lazy-loadable))))))

(defn function [lazy-loadable]
  (fn lazy-wrapper [& args]
    (p/let [f (unpack lazy-loadable)]
      (apply f args))))
