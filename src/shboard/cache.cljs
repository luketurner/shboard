(ns shboard.cache
 (:require [cljs.core.async :refer [<!]]
           [shboard.apis.node.process :refer [env-var]]
           [clojure.string :as string]
           [shboard.apis.node.fs :refer [read-file]])
 (:require-macros [cljs.core.async.macros :refer [go]]))

; (defn read-cached-value!
;  "Retrieves a value from the local filesystem cache.
;   Returns a promise channel."
;  [key]
;  (go
;   (let [cached-data (-> 
;                         (read-file)
;                         (<!)
;                         (or "")
;                         (reader/read-string))]
;    (get-in cached-data key))))