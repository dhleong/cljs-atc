(ns atc.voice.process
  (:require
   [atc.voice.parsing.numbers :as numbers]
   [clojure.string :as str]
   [instaparse.core :as insta]))

; TODO: Declaring the grammar in code is convenient, but we will probably want to
; generate it at compile time, dump it to a file, and load that file in production
; instead for performance...
(def fsm
  (delay
    (time
      (insta/parser
        (str/join "\n"
                  (concat
                    numbers/rules
                    ["<whitespace> = <' '>"]))))))

(defn find-command [input]
  (@fsm input))

(comment
  (println (find-command "one"))
  (println (find-command "one zero two"))
  (println (find-command "two twenty one")))
