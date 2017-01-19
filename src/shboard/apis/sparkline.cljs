(ns shboard.apis.sparkline
 (:require [cljs.nodejs :refer [require]]))

(def js-sparkline (require "sparkline"))

(defn sparkline-from-seq
 "Accepts a list of numbers and returns a string representing a sparkline for that data."
 [data]
 (if (some? data)
  (js-sparkline (clj->js data))
  ""))