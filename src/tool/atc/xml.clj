(ns atc.xml 
  (:require
   [clojure.zip :as zip]
   [clojure.string :as str]
   [clojure.data.zip.xml :as z]))

(defn- right-to-next [start tag]
  (loop [loc start]
    (when loc
      (if (= tag (:tag (zip/node loc)))
        loc
        (recur (zip/right loc))))))

(defn down-to-first [tag]
  (with-meta
    (fn produce [loc]
      (if (= tag (:tag (zip/node loc)))
        [loc]
        (when-let [start (zip/down loc)]
          (when-let [found (right-to-next start tag)]
            [found]))))
    {:name [:down-to-first tag]}))

(defn down-to-every [tag]
  (with-meta
    (fn produce [loc]
      (when-let [start (zip/down loc)]
        (letfn [(produce-next [start]
                  (right-to-next start tag))
                (produce-seq [start]
                  (when-let [up-next (produce-next start)]
                    (cons up-next (lazy-seq (produce-seq up-next)))))]
          (produce-seq start))))
    {:name [:down-to-every tag]}))

(defn- describe-path-entry [path-entry]
  (-> path-entry meta :name))

(defn- scan1 [root pred]
  (pred root))

(defn- lazy-scan [roots path]
  (if-not (seq path)
    roots ; These are our results!

    (do
      (println "Look into" (describe-path-entry (first path)) (count roots))
      (recur
        (mapcat
          (fn [root]
            (println "scan" (:tag (zip/node root)) "for" (describe-path-entry (first path)))
            (lazy-seq
              (scan1 root (first path))))
          roots)
        (next path)))))

(defn- compile-path-entry [path-entry]
  (cond
    (keyword? path-entry) (let [n (name path-entry)]
                           (cond
                             (str/ends-with? n "+") (down-to-every
                                                      (keyword
                                                        (namespace path-entry)
                                                        (subs n 0 (dec (count n)))))
                             :else (down-to-first path-entry)))
    (fn? path-entry) path-entry
    :else (throw (ex-info "Unsupported path specifier" {:given path-entry}))))

(defn scan-> [root & path]
  (lazy-scan [root] (map compile-path-entry path)))

(defn tag= [tag]
  (fn tag= [loc]
    (= (:tag loc) tag)))

(defn tag-content= [tag expected]
  (fn tag-content= [loc]
    (and (= tag (:tag loc))
         (= (first (:content loc))
            expected))))

(defn- compile-scanner-pred [pred]
  (cond
    (keyword? pred) (tag= pred)
    (fn? pred) pred
    :else (throw (ex-info (str "Invalid predicate:" pred) {:pred pred}))))

(defn scan-until [scanner pred]
  (let [pred (compile-scanner-pred pred)]
    (loop [aseq (scanner 0)
           scope (scanner 1)]
      (let [current (first aseq)
            next-scope (if (:tag current)
                         (assoc scope (:tag current) current)
                         scope)]
        (if (and (not (identical? scope next-scope))
                 (pred current))
          [aseq next-scope]
          (recur (next aseq) next-scope))))))

(defn scope
  ([scanner] (scanner 1))
  ([scanner tag]
   (get (scope scanner) tag)))

(defn ->scanner
  "Given an xml-seq, create a scanner object that tracks scope by most-recent tags"
  [aseq]
  [aseq {}])

(comment
  #_{:clj-kondo/ignore [:unresolved-namespace]}
  (with-open [reader (clojure.java.io/reader "~/Downloads/AIXM_5.1/XML-Subscriber-Files/APT_AIXM.xml")]
    (-> reader
        (clojure.data.xml/parse
          :namespace-aware false)
        xml-seq
        ->scanner
        (scan-until (tag-content= :aixm:locationIndicatorICAO "KJFK"))
        ; zip/xml-zip
        #_(z/xml1->
            :faa:SubscriberFile
            :faa:Member
            :aixm:AirportHeliport
            :aixm:timeSlice
            :aixm:locationIndicatorICAO)
        #_(scan->
            :faa:SubscriberFile
            :faa:Member+
            :aixm:AirportHeliport
            :aixm:timeSlice
            #_:aixm:locationIndicatorICAO)
        #_first
        ; zip/node
        #_(nth 21)
        #_:tag
        second
        :aixm:locationIndicatorICAO ))
  )
