(ns shboard.ui.header
 (:require [shboard.apis.blessed :as blessed :refer [text
                                                     layout]])
 (:use [clojure.string :only [join]]))

(def header-height 3)


(def logo-text
  (->> 
   ["//   shboard   // :: [{white-fg}Q{/}] quit shboard     [{white-fg}R{/}] reload server list"
    "All your servers  :: [{white-fg}q{/}] close window     [{white-fg}r{/}] reload stats"
    "are belong to us. ::"]
   (join \newline)))
  

(defn header-layout
  "Returns a Blessed object for the primary shboard header"
  [opts]
  (let [logo (text logo-text {:width "100%"
                              :height header-height
                              :style (:style opts)
                              :options {:tags true}})]
    (layout {:width "100%"
             :height header-height
             :style (:style opts)
             :children [logo]})))