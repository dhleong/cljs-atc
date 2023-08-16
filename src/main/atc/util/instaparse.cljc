(ns atc.util.instaparse
  (:require
   [atc.voice.parsing.core :refer [declare-alternates]]
   [clojure.string :as str]
   [clojure.walk :as walk]
   [instaparse.core :as insta]))

(defn compile-grammar [parseable-grammar]
  (->> (insta/parser parseable-grammar)
       (walk/postwalk
         (fn [form]
           (cond
             ;; Lists cannot be evaluated verbatim
             (seq? form)
             (list* 'list form)

             :else form)))
       :grammar))

(defn generate-grammar-nops [nop-names]
  ; NOTE (name n) doesn't work in the defrules macro for some reason
  (map (fn [n] (cond-> (str n " = 'nop'")
                 (keyword? n) (subs 1)))
       nop-names))

(defn extract-grammar [v]
  (or (:grammar (meta v))
      (:grammar v)))

(defmacro defalternates
  "The name can be tagged with ^:hide-tag to hide the tag from the output.
   This is equivalent to declaring the rule with <angle brackets> around the
   tag name."
  [name value]
  (let [alternates (if (map? value)
                     (keys value)
                     value)
        hide-tag (fn [s] (str "<" s ">"))
        tag-name (cond-> (str name)
                   (:hide-tag (meta name)) (hide-tag))
        parseable-grammar (declare-alternates tag-name alternates)]
    `(def ~name (with-meta
                  ~value
                  {:grammar ~(compile-grammar parseable-grammar)}))))

(defn format-alternate-expr [v]
  (cond
    (string? v)
    {:tag :string, :string v}

    (keyword? v)
    {:tag :nt :keyword v}))

(defn generate-alternatives-grammar [kw hide-tag? expr]
  {:grammar
   {kw
    {:tag :alt
     :red (if hide-tag?
            {:reduction-type :raw}
            {:reduction-type :hiccup, :key kw})
     :parsers (cond->> expr
                (map? expr) keys
                true (map format-alternate-expr))}}})

(defmacro defalternates-expr [name expr]
  (let [kw (keyword (str name))]
    `(def ~name (let [v# ~expr]
                  (with-meta
                    v#
                    (generate-alternatives-grammar
                      ~kw ~(:hide-tag (meta name))
                      v#))))))

(defn- format-dependencies-map [grammar dependencies-map]
  (if-some [deps (->> dependencies-map
                      vals
                      (keep (fn [v]
                              (when v
                                `(extract-grammar ~v))))
                      seq)]
    (concat `(merge ~grammar) deps)
    grammar))

(defmacro defrules
  "Declare a set of composed rules. `dependencies` may be a list of
   rules that will be merged in later, or a map of {dependency rule}
   to merge in those dependencies directly"
  ([name string-rules]
   `(defrules ~name ~string-rules nil))
  ([name string-rules dependencies]
   (let [nop-names (if (map? dependencies)
                     (keys dependencies)
                     dependencies)
         provided-rules (if (string? string-rules)
                          [string-rules]
                          string-rules)
         compiled (-> provided-rules
                      (concat
                        (generate-grammar-nops nop-names))
                      (->> (str/join "\n"))
                      (compile-grammar)
                      (as-> g
                        (apply dissoc g nop-names)))
         grammar (cond-> compiled
                   (map? dependencies)
                   (format-dependencies-map dependencies))]
     `(def ~name {:grammar ~grammar}))))
