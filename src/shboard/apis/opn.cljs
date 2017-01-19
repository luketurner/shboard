(ns shboard.apis.opn
 (:require [cljs.nodejs :refer [require]]))

(def js-opn (require "opn"))

(defn opn
 "Opens a URL/file/program."
 [string]
 (js-opn string))