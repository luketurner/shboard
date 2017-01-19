(ns shboard.colors
 (:require [cljs.spec :as spec]
           [shboard.config :as config :refer [get-config-value]]
           [shboard.apis.node.crypto :refer [md5]]
           [shboard.apis.blessed :as blessed]))

(spec/fdef get-color
 :args (spec/cat :path (spec/or :vec vector? :kw keyword?))
 :ret (spec/nilable :blesesd/color))

(defn get-style
 "Gets a style from the color scheme, based on its key.
  e.g. (get-color :active) -> {:fg ... :bg ...}"
 [color-key]
 (get-config-value [:color-scheme color-key]))


; with let + atom, we can get functions with private, mutable state.
; why we would want to do that, I have no idea...
(let [index (atom 0)
      memo  (atom {})]
 (defn pick-value-with-string
  "Picks a value from given vector based on given string.
   The same string will always return the same color from the
   same set of choices."
   [choices string]
   (let [memo-index (get @memo string)
         max-index  (count choices)
         index      (or memo-index (swap! index inc))]
    (when-not memo-index 
     (swap! memo assoc string index))
    (get choices (mod index max-index)))))

(defn string->color
 "Given a string, returns a representation of that string as a color.
  Utilizes the :color-wheel setting to determine what algorithm to use
  for mapping strings to colors."
 [string]
 (let [color-wheel (get-config-value [:color-scheme :color-wheel])]
  (condp apply [color-wheel]
   vector?  (pick-value-with-string color-wheel string)
   keyword? (case color-wheel 
                  :automatic (->> string
                                  (str)
                                  (md5)
                                  (take 6)
                                  (apply str "#"))))))