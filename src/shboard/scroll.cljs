(ns shboard.scroll
 (:require [shboard.util :refer [same-values?]]
           [shboard.log :refer [log]]))

(def offsets
 (atom {}))

(defn get-offset
 [key]
 (get @offsets key 0))

(defn set-offset!
 ([key offset max-offset] 
  (set-offset! key (min offset max-offset)))
 ([key offset]
  (swap! offsets assoc key (max offset 0))))


; TODO - is there a less repetitive way to write this?
(defn add-to-offset!
 ([key num max-offset]
  (set-offset! key (+ num (get-offset key)) max-offset))
 ([key num]
  (set-offset! key (+ num (get-offset key)))))

(defn add-scroll-watch
 "Creates a watcher that's called whenever the given scroll-key is scrolled.
  The watch-key should be as a unique identifier that can be used to remove the watcher."
 [scroll-key watch-key handler]
 (add-watch offsets watch-key
  (fn [_ _ old new]
   (when-not (same-values? scroll-key old new)
    (log "scroll/add-scroll-watch calling:" watch-key "for:" scroll-key)
    (handler (get new key))))))