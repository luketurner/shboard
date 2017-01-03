(ns shboard.new-relic
  (:require [shboard.state :as state])
  (:use [cljs.pprint :only [pprint]]))

(def request (js/require "request"))

(defn get-api-key
  "Retrieves the New Relic API key from the global state db"
  []
  (get-in @state/db [:config :new-relic-api-key]))

(defn new-relic-request
  [opts callback]
  (let [api-key (get-api-key)
        cb-wrapper #(if (%1) (throw %1) (callback %3))]
    (println "api-key" api-key)
    (-> opts
      (assoc-in [:headers "X-Api-Key"] api-key)
      (assoc :json true)
      (clj->js)
      (request callback))))

(defn fetch-server-stats!
  "Retrieves the 10 latest stats for given server(s) from New Relic"
  [server-hostname new-relic-id]
  (let [api-key (get-api-key)
        request-options {:url (str "https://api.newrelic.com/v2/servers/" new-relic-id "/metrics/data.json")
                         :qs {"names" ["System/Network/All/All/bytes/sec"
                                       ;"System/Disk/All/Writes/bytes/sec"
                                       ;"System/Disk/All/Reads/bytes/sec"
                                       "System/CPU/System/percent"
                                       "System/Memory/Used/bytes"
                                       "System/Disk/All/Utilization/percent"]
                          :qsParseOptions {:arrayFormat :brackets}
                          :period 60}}]
    (new-relic-request request-options
      (fn [err result body]
        (println "got response" (js->clj body)))))); TODO - do something

(defn fetch-server-ids!
  "Retrieves and stores a map from the server hostname to the New Relic ID"
  [server-hostnames]
  (let [request-options {:url "https://api.newrelic.com/v2/servers.json"
                         :qs {"filter[reported]" true}}]
    (new-relic-request request-options
      (fn [err result body]
        (when (not (nil? err)) (println err))
        (let [{servers "servers" :as body} (js->clj body)
              server-id-map (->> servers
                                 (map (fn [server] [(get server "host") (get server "id")]))
                                 (into {}))]
          (swap! state/db assoc-in [:new-relic-server-ids] server-id-map))))))
          
(add-watch state/db :new-relic-watcher
  (fn [_ atom old new]
    (let [new-server-ids (:new-relic-server-ids new)
          old-server-ids (:new-relic-server-ids old)]
      (when (not (= new-server-ids old-server-ids))
        (println "Getting server stats" new-server-ids)
        (doseq [[host id] new-server-ids] (fetch-server-stats! host id ))))))