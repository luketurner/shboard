(ns shboard.apis.node.path
 (:require [cljs.nodejs :refer [require]]))

(def js-path (require "path"))

(defn dirname
 "Gets the directory portion of a path (e.g. /home/luke/asdf.ext -> /home/luke)"
 [path]
 (.dirname js-path path))