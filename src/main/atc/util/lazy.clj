(ns atc.util.lazy
  (:require
   [shadow.lazy :as lazy]))

; Re-exporting for convenience
#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defmacro loadable [s]
  `(lazy/loadable ~s))

