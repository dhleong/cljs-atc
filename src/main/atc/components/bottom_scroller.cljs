(ns atc.components.bottom-scroller
  (:require
   [applied-science.js-interop :as j]
   [react :as React]))

(defn- bottom-scroller-f> [opts children]
  (let [bottom-el-ref (React/useRef)]
    (React/useEffect
      (fn []
        (when-let [bottom-el (j/get bottom-el-ref :current)]
          (.scrollIntoView bottom-el #js {:behavior "smooth"}))))

    (-> [:div opts]
        (into children)
        (conj [:div.bottom {:ref bottom-el-ref}]))))

(defn bottom-scroller [opts & children]
  [:f> bottom-scroller-f> opts children])
