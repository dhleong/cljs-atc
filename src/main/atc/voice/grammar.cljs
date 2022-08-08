(ns atc.voice.grammar
  (:require
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


(defn from-command-model [{:keys [model root-rule amount]
                           :or {root-rule :command
                                amount 10}}]
  (let [generate-ctx {:root model
                      :subject (get model root-rule)}]
    (->> (range amount)
         (map (fn [_] (->> (generate-from-model generate-ctx)
                           (str/join " ")))))))

(defn generate [command-model]
  (->> (concat
         (from-command-model {:amount 200
                              :model command-model})
         ["[unk]"])
       to-array
       (js/JSON.stringify)))
