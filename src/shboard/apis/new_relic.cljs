(ns shboard.apis.new-relic
  (:require [shboard.log :refer [log]]
            [shboard.servers :as servers]
            [shboard.config :as config]
            [cljs.nodejs :refer [require]])
  (:use [clojure.set :only [difference]]))

(def js-request (require "request"))

(def server-id-table 
 "Holds a map from server hostnames to their corresponding New Relic IDs.
  If a server is not listed here, assume that it is not available in New Relic Servers."
 (atom {}))

(defn get-api-key
  "Retrieves the New Relic API key."
  []
  (config/get-config-value [:api-keys :new-relic]))

(defn- new-relic-request
 "Constructs a request for the New Relic API. Calls callback with response."
 [opts callback]
 (let [api-key (get-api-key)
       cb-wrapper (fn [e r d]
                   (if e (throw e)
                    (do
                     (callback (js->clj d)))))
       request-options (-> opts
                           (assoc-in [:headers "X-Api-Key"] api-key)
                           (assoc :json true)
                           (clj->js))]
  (js-request request-options cb-wrapper)))

(defn- get-metric-data
  "Extracts a list of the values for a metric from a New Relic response."
  [metric-name value-name data]
  (let [{{metrics "metrics"} "metric_data"} data
        metric-entry (first (filter #(= metric-name (get % "name")) metrics))
        timeslices (get metric-entry "timeslices")
        metric-values (map #(get-in % ["values" value-name]) timeslices)]
    metric-values))

(defn fetch-server-stats
 "Retrieves the 10 latest stats for given server(s) from New Relic. Returns nil if server is not yet in table."
 [server-hostname callback]
 (if-let [new-relic-id (get @server-id-table server-hostname)]
   (let [api-key (get-api-key)
         request-options {:url (str "https://api.newrelic.com/v2/servers/" new-relic-id "/metrics/data.json")
                           :qs {"names" ["System/Network/All/All/bytes/sec"
                                    ;"System/Disk/All/Writes/bytes/sec"
                                    ;"System/Disk/All/Reads/bytes/sec"
                                           "System/CPU/System/percent"
                                           "System/Memory/Used/bytes"
                                           "System/Disk/All/Utilization/percent"]
                                 :qsParseOptions {:arrayFormat :brackets}
                                 :period 30}}]
     (log "new-relic/fetch-server-stats!: fetching stats for: " server-hostname)
     (new-relic-request request-options
       (fn [data]
         (let [server-stats {:cpu (get-metric-data "System/CPU/System/percent" "average_value" data)
                             :memory (get-metric-data "System/Memory/Used/bytes" "average_value" data)
                             :disk (get-metric-data "System/Disk/All/Utilization/percent" "average_value" data)
                             :net (get-metric-data "System/Network/All/All/bytes/sec" "per_second" data)}]
           (log "new-relic/fetch-server-stats!: got" (apply + (map (comp count second) server-stats)) "stats for:" server-hostname)
           (callback server-stats))))
     (callback nil))))
  

(defn update-server-ids!
  "Retrieves and stores a map from the server hostname to the New Relic ID"
  [servers]
  (let [request-options {:url "https://api.newrelic.com/v2/servers.json"
                         :qs {"filter[reported]" true}}
        server-hostnames (into #{} (map :shboard.servers/private-dns-name servers))]
   (log "new-relic/fetch-server-ids!: fetching ids for: " server-hostnames)
   (new-relic-request request-options
     (fn [data]
       (let [{servers "servers"} data
             server-id-map (into {} (for [{host "host" id "id"} servers :when (contains? server-hostnames host)] [host id]))]
         (if (< 0 (count server-id-map)) 
          (log "new-relic/fetch-server-ids!: got server ids: " server-id-map)
          (log "new-relic/fetch-server-ids!: got empty id set. Response:" data))
         (reset! server-id-table server-id-map))))))