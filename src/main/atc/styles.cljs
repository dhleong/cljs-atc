(ns atc.styles
  (:require
   [spade.core :refer [defglobal]]))

#_:clj-kondo/ignore ; ignore that this global style is unused
(defglobal window-styles
  [:body {:margin 0
          :padding 0}])
