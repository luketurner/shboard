(ns shboard.apis.node.fs
 (:require [cljs.nodejs :refer [require]]
           [cljs.core.async :refer [put! promise-chan]]
           [shboard.apis.node.process :refer [env-var]]
           [clojure.string :as string])
 (:require-macros [cljs.core.async.macros :refer [go]]))

(def js-fs (require "fs"))

(defn resolve-home
 "Replaces instances of ~ in the path with $HOME if it exists."
 [path]
 (if-let [HOME (env-var "HOME")]
  (string/replace path #"^~/" (str HOME "/"))
  path))
  

(defn read-file
 "Reads a file asynchronously. Returns a promise of a string."
 [file]
 (let [file    (resolve-home file)
       promise (promise-chan)]
  (.readFile js-fs file "utf8" (fn [err data] 
                                (put! promise (if (some? err) false data))))
  promise))

(defn write-file
 "Writes a file asynchronously.
  Returns an error promise, which may receive an error, or 'false' if successful."
 [file content]
 (let [file    (resolve-home file)
       promise (promise-chan)]
  (.writeFile js-fs file content
   (fn [err data] 
    (put! promise (if (some? err) err false))))
  promise))

(defn mkdir
 "Creates a directory asynchronously. Returns an error promise.
  If the directory already exists, nothing is done and no error is returned."
  [directory]
 (let [directory (resolve-home directory)
       promise   (promise-chan)]
  (if (.existsSync js-fs directory) ; TODO - make this use an async method?
   (put! promise false)
   (.mkdir js-fs directory
    (fn [err data] 
     (put! promise (if (some? err) err false)))))
  promise))