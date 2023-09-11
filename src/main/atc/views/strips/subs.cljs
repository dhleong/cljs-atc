(ns atc.views.strips.subs
  (:require
   [re-frame.core :refer [reg-sub]]))

(reg-sub
  ::context
  :-> :flight-strips)

(reg-sub
  ::state
  :<- [::context]
  :-> :state)

(reg-sub
  ::strips
  :<- [:game/airport]
  :<- [:game/tracked-aircraft]
  :-> (fn [[airport aircraft]]
        (->> aircraft
             (map
               (fn [craft]
                 (-> craft
                     (dissoc :position :heading :speed)
                     (assoc :arrival? (= (:id airport)
                                         (:destination craft)))))))))

(reg-sub
  ::arrival-strips
  :<- [::strips]
  :-> (partial filter :arrival?))

(reg-sub
  ::departure-strips
  :<- [::strips]
  :-> (partial remove :arrival?))
