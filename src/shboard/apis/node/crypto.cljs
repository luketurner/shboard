(ns shboard.apis.node.crypto
 (:require [cljs.nodejs :refer [require]]))

(def js-crypto (require "crypto"))

(defn hash-digest
 "Returns a digest of the hash of given string using given hash-type (e.g. sha512) and digest-type (e.g. hex)"
 [hash-type digest-type string]
 (.digest (doto (.createHash js-crypto hash-type) (.update string)) digest-type))

(def md5 
 "Returns the md5 hash of a string"  
 (partial hash-digest "md5" "hex"))