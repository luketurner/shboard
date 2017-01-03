(ns shboard.state)

(def default-db-state 
  "Defines the default content for the state db atom."
  {:servers [{:id "Loading, please wait..."
              :name "Loading, please wait..."
              :class :none
              :subclass :none
              :state :missing
              :launch-time ""
              :private-ip ""
              :private-dns-name ""
              :public-ip ""
              :public-dns-name ""}]
   :debug? false
   :config {:new-relic-api-key nil}})

(def db 
  "Provides centralized storage for all of shboard's data.
   All UI components should read from it, so that asynchronous data updates
   are reflected in the UI automatically."
  (atom default-db-state))


(defn add-debug-watcher!
  "Adds a watcher to the DB atom that logs every DB update."
  []
  (add-watch db nil #(println "[DEBUG] shboard/state: update detected.")))