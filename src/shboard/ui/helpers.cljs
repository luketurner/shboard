(ns shboard.ui.helpers
 (:require [cljs.pprint :refer [cl-format]]
 	       [shboard.ui.screen :refer [screen]]
		   [shboard.util :refer [in-range?]]))

(defn readable-percent
 "Accepts a value like 12.1234 and converts it to 12%"
 [cpu]
 (cl-format nil "~4,1f%" (.round js/Math cpu)))

(defn readable-bytes
 "Accepts a raw numeric value in bytes and returns a compact human-readable version with units."
 [mem]
 (let [[power, unit] (condp #(apply in-range? %2 %1) mem
                      [0         100000]       [10, "k"]
                      [100000    100000000]    [20, "m"]
                      [100000000 100000000000] [30, "g"]
                                               [40, "t"])
       scaled-memory (/ mem (.pow js/Math 2 power))]
  (cl-format nil "~4,1f~a" scaled-memory unit)))

(defn render-into!
 "Sets the content of given element and re-renders to update the screen immediately."
 [target-element new-content]
 (.setContent target-element new-content)
 (.render @screen))