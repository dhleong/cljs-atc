(ns atc.components.icon
  (:require
   [clojure.string :as str]))

(defmacro icon
  "A material design icon. `spec` is a keyword
  that is usually the name of the icon, but
  can also have .class like normal hiccup"
  [spec & [opts]]
  {:pre [(keyword? spec)]}
  (let [spec (name spec)
        class-offset (.indexOf spec ".")
        classes (when (not= -1 class-offset)
                  (subs spec class-offset))
        icon-name (str/replace
                    (if (not= -1 class-offset)
                      (subs spec 0 class-offset)
                      spec)
                    #"-"
                    "_")]
    `[~(keyword (str "span.material-symbols-outlined" classes)) ~(or opts {}) ~icon-name]))
