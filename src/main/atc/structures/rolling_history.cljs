(ns atc.structures.rolling-history
  (:require [cljs.core :refer [APersistentVector]]))

(defprotocol IHistorical
  (-most-recent-n [coll n] "Like take-last but O(1)"))

(deftype RollingHistory [^APersistentVector -vec max-count]
  ICollection
  (-conj [_this v]
    (let [new-vec (if (>= (count -vec) max-count)
                    (conj (subvec -vec 1) v)
                    (conj -vec v))]
      (RollingHistory. new-vec max-count)))

  IEmptyableCollection
  (-empty [_this] (RollingHistory. (empty -vec) max-count))

  IStack
  (-peek [_this]
    (peek -vec))
  (-pop [this]
    (when-not (= 0 (count this))
      (RollingHistory. (pop -vec) max-count)))

  ICounted
  (-count [_this]
    (count -vec))

  ISeqable
  (-seq [_this]
    (seq -vec))

  IHistorical
  (-most-recent-n [_this n]
    (let [start (max 0 (- (count -vec) n))]
      (subvec -vec start)))

  IPrintWithWriter
  (-pr-writer [_this writer opts]
    (-write writer "#history ")
    (-pr-writer -vec writer opts)))

(defn most-recent-n [n ^IHistorical history]
  (-most-recent-n history n))

(defn rolling-history [max-count]
  (->RollingHistory [] max-count))
