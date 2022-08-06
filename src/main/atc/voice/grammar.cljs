(ns atc.voice.grammar
  (:require
   [atc.voice.process :as process]
   [clojure.string :as str]))

(defmulti generate-from-model (comp :tag :subject))

(defmethod generate-from-model
  :alt
  [{{:keys [parsers]} :subject :as ctx}]
  (generate-from-model (assoc ctx :subject (rand-nth parsers))))

(defmethod generate-from-model
  :cat
  [{{:keys [parsers]} :subject :as ctx}]
  (->> parsers
       (mapcat #(generate-from-model (assoc ctx :subject %)))))

(defmethod generate-from-model
  :opt
  [{{:keys [parser]} :subject :as ctx}]
  (when-not (= :whitespace (:keyword parser))
    (when (> (rand) 0.5)
      (generate-from-model (assoc ctx :subject parser)))))

(def ^:private plus-repetition-candidates [2 3 4 5])

(defmethod generate-from-model
  :plus
  [{{:keys [parser]} :subject :as ctx}]
  ; Would be nice to have an indication of limit...
  (->> (range 1 (rand-nth plus-repetition-candidates))
       (mapcat (fn [_] (generate-from-model (assoc ctx :subject parser))))))

(defmethod generate-from-model
  :nt
  [{{next-tag :keyword} :subject root :root}]
  (generate-from-model {:root root
                        :subject (get root next-tag)}))

(defmethod generate-from-model
  :string
  [{{:keys [string]} :subject}]
  (list string))

(defmethod generate-from-model
  :default
  [{subject :subject}]
  (println "Unexpected subject? " (:tag subject))
  nil)


(defn from-command-model
  ([] (from-command-model nil))
  ([{:keys [model root-rule amount]
     :or {root-rule :command
          amount 10}}]
   (let [model (or model (process/grammar))
         generate-ctx {:root model
                       :subject (get model root-rule)}]
     (->> (range amount)
          (map (fn [_] (->> (generate-from-model generate-ctx)
                            (str/join " "))))))))

; (defn- generate []
;   (let [digits ["zero" "one" "two" "three" "four" "five" "six" "seven" "eight" "nine"]
;         double-digits (concat
;                         ["ten" "eleven" "twelve" "thirteen" "fourteen" "fifteen" "sixteen" "seventeen" "eighteen" "nineteen"]
;                         (for [ten ["twenty" "thirty" "forty" "fifty" "sixty" "seventy" "eighty" "ninety"]
;                               d digits]
;                           (str ten " " d)))

;         airlines ["delta" "speed bird" "united"]
;         airline-callsigns (concat
;                             (for [a airlines
;                                   d digits
;                                   dd double-digits]
;                               (str a " " d " " dd))

;                             (for [a airlines
;                                   d1 double-digits
;                                   d2 double-digits]
;                               (str a " " d1 " " d2)))]

;     (->> (concat
;            airline-callsigns
;            ["contact tower"]
;            ["turn left heading two zero zero"]
;            ["[unk]"])
;          to-array
;          (js/JSON.stringify))))

(defn generate []
  (->> (concat
         (from-command-model {:amount 200})
         ["[unk]"])
       to-array
       (js/JSON.stringify)))
