(ns atc.fx
  (:require [re-frame.core :refer [reg-fx]]
            [archetype.nav :as nav]))

(reg-fx
  :nav/replace!
  nav/replace!)
