(ns atc.weather.metar
  (:require
   [atc.util.numbers :refer [->int]]
   [clojure.string :as str]
   [instaparse.core :as insta :refer-macros [defparser]]))

(defparser ^:private metar-parser
  "<metar> = airport time wind visibility-sm clouds temp-dewpoint altimeter
   airport = letter+
   time = numbers <'Z'>
   wind = wind-direction wind-kts (<'G'> numbers)? <'KT'>
   clouds = cloud*
   temp-dewpoint = temperature <'/'> temperature
   altimeter = <'A'> number number number number

   cloud = #'\\w+'
   temperature = 'M'? numbers
   wind-direction = number number number
   wind-kts = number number
   visibility-sm = numbers <'SM'>

   <letter> = #'[A-Z]'
   numbers = number+
   <number> = #'[0-9]'"
  :auto-whitespace :standard)

(defn- parse-numbers [& numbers]
  (->int (str/join numbers)))

(def ^:private transformers
  {:temperature (fn
                  ([temp] temp)
                  ([_minus temp]
                   (- temp)))
   :wind-direction parse-numbers
   :wind-kts parse-numbers
   :numbers parse-numbers})

(defn parse-text [raw-metar]
  (let [parts (->> (metar-parser raw-metar :partial true)

                   ; NOTE: clj-kondo seems confused but this fn definitely exists!
                   #_{:clj-kondo/ignore [:unresolved-var]}
                   (insta/transform transformers)
                   (map (fn [[k & v]]
                          [k v]))
                   (into {}))
        {:keys [altimeter wind visibility-sm temp-dewpoint]} parts]
    {:altimeter (let [[a b c d] altimeter]
                  (str a b "." c d))
     :dewpoint-c (second temp-dewpoint)
     :temperature-c (first temp-dewpoint)
     :visibility-sm (first visibility-sm)
     :wind-heading (first wind)
     :wind-kts (second wind)}))
