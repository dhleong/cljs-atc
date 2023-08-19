(ns atc.voice.parsing.core
  (:require
   [clojure.string :as str]))

(defn declare-alternates [rule-name values]
  (->> values
       (map #(str "'" % "'"))
       (str/join " | ")
       (str rule-name " = ")))

