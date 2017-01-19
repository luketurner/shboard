(ns shboard.apis.clipboard
 (:require [cljs.nodejs :refer [require]]))

(def js-copy-paste (require "copy-paste"))

(defn write
 "Writes some content to the clipboard."
 [string]
 (.copy js-copy-paste string))