(ns atc.data.airports.kjfk)

(def airport
  {:name "Kennedy International"
   :magnetic-north -13.
   :position [:N40.63992778 :W73.77869167 13]
   :navaids [{:id "COL"
              :type :vor
              :pronunciation "colts neck"
              :position [:N40*18'41.866 :W74*09'35.023]}
             {:id "JFK"
              :type :vor
              :pronunciation "kennedy"
              :position [:N40*37'58.4 :W73*46'17]}
             {:id "LGA"
              :type :vor
              :pronunciation "la guardia"
              :position [:N40*47'01.376 :W73*52'06.962]}
             {:id "MERIT"
              :type :fix
              :pronunciation "merit"
              :position [:N41.381833, :W73.137333]}]})
