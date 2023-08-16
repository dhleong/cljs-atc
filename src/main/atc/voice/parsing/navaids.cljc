(ns atc.voice.parsing.navaids
  (:require
   [atc.util.instaparse :refer-macros [defrules]]
   [clojure.string :as str]))

(defrules rules
  ["<navaid> = navaid-pronounced | navaid-spelled"
   "navaid-spelled = (letter letter letter <'v o r'>?) | (letter letter letter letter letter <'intersection'>?)"]
  [:letter :navaid-pronounced])

(def transformers
  {:navaid-spelled (fn [& letters]
                     (str/join letters))})
