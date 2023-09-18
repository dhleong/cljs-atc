(ns atc.components.help-span
  (:require
   [archetype.util :refer [>evt]]
   [clojure.string :as str]))

(defn- create-help-attrs [kind]
  {:on-context-menu (fn [e]
                     (.preventDefault e)
                     (>evt [:help/identify-span
                            kind
                            (.. e -target -innerText)]))})

(defn help-span [help-key-or-opts & content]
  (let [element (:as help-key-or-opts :span)
        help-key (or (when (keyword? help-key-or-opts)
                       help-key-or-opts)
                     (:key help-key-or-opts)
                     (some-> (name element)
                             (str/split #"\.")
                             (last)
                             (keyword))
                     (throw (ex-info "No help key on help-span"
                                     {:opts help-key-or-opts
                                      :content content})))]
    (into [element (create-help-attrs help-key)] content)))
