(ns shboard.ui.state
 (:require [clojure.spec :as spec]
           [shboard.log :refer [log]]
           [cljs.nodejs :refer [require]]))

(def _throttle (require "lodash.throttle"))

(def default-db-state 
  "Defines the default content for the state db atom."
  {:log-view :none
   :dashboard {:selected nil}})

(def db 
 "Provides centralized storage for all of shboard's UI data.
  All UI components should read from it, so that asynchronous data updates
  are reflected in the UI automatically."
 (atom default-db-state))

(defn get-state-value
 "Gets state-key (which can be a path vector or a plain key) from the state db.
  Accepts an optional default value that will be returned if the key does not exist in the db."
 ([state-key] ((if (vector? state-key) get-in get) @db state-key))
 ([state-key default] ((if (vector? state-key) get-in get) @db state-key default)))

(defn set-state-value!
 "Sets a state-key (which can be a path vector or a plain key) into the state db."
 ([state-key new-val] (set-state-value! state-key new-val false))
 ([state-key new-val render?]
  (println "set-state-value!" state-key new-val)
  (swap! db
         #(-> %
           ((if (vector? state-key) assoc-in assoc) state-key new-val)
           (assoc :render-requested? render?)))))

(defn- request-render-unthrottled
 []
 (println "request-render!")
 (swap! db assoc :render-requested? true))

(def request-render!
 "Requests a re-render without updating any state values.
  Useful when when handling data stored outside UI state (e.g. metrics).
  Unlike set-state-value!, request-render! is throttled to 20 fps."
 (_throttle request-render-unthrottled 16))
 

(defn add-render-watch
 "Creates a watcher that's called whenever a render is requested."
 [watch-key handler]
 (add-watch db watch-key
  (fn [_ _ _ new]
   (when (get new :render-requested?)
    (log "state/add-render-watch calling:" watch-key)
    (handler new)
    (swap! db assoc :render-requested? false))))) 
