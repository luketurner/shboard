(ns shboard.server-stats
 (:require [cljs.spec :as spec]
           [shboard.servers :as servers]
           [shboard.apis.new-relic :as new-relic]
           [shboard.util :refer [explain-valid?]]
           [shboard.config :refer [get-config-value]]
           [shboard.log :refer [log]]))

(spec/def ::cpu (spec/* number?))
(spec/def ::memory (spec/* number?))
(spec/def ::disk (spec/* number?))
(spec/def ::net (spec/* number?))

(spec/def ::server-stats (spec/keys :req [::cpu ::memory ::disk ::net]))
                                          
(spec/def ::server-stats-index (spec/map-of :shboard.servers/id ::server-stats))

(defn metrics-enabled?
 "Returns false if --no-metrics was paeed, true otherwise."
 []
 (not (get-config-value [:flags :no-metrics])))

(def server-stats
 "Stores statistical (i.e. dynamic, regularly-updated) information about servers."
 (atom {} :validator #(explain-valid? ::server-stats-index %)))

(defn get-stats-for-server
 [server-id]
 (get @server-stats server-id))

(defn get-stat-for-server
 [server-id stat]
 (get-in @server-stats [server-id stat]))

(defn update-server-stats!
 [{:keys [shboard.servers/private-dns-name shboard.servers/id] :as server}]
 (if (metrics-enabled?)
  (do 
   (log "server-stats/update-server-stats!: updating stats for server:" id)
   (new-relic/fetch-server-stats private-dns-name
    (fn [{:keys [cpu disk memory net] :as stats}]
     (if stats
      (swap! server-stats assoc id {::cpu cpu ::disk disk ::memory memory ::net net})))))
  (println "server-stats/update-server-stats!: metrics disabled")))
   
(defn update-all-stats!
 []
 (if (metrics-enabled?)
  (doseq [server (servers/get-all-servers)]
   (update-server-stats! server))
  (println "server-stats/update-all-stats!: metrics disabled")))

; (defn start-polling!
;  [interval]
;  (log "server-stats/start-polling!: Started polling every" interval "seconds")
;  (timers/every-thirty-seconds :server-stats/update-all-stats! update-all-stats!))
 
(defn add-stat-watcher
 "Adds a watcher for the given server and stat.
  Watcher is called with seq of new stats."
  [server stat watch-key handler]
  (add-watch server-stats watch-key
   (fn [_ _ old new]
    (let [new-stat (get-in new [server stat])
          old-stat (get-in old [server stat])]
     (when (and (some? new-stat)
                (not= old-stat new-stat))
      (log "server-stats/add-stat-watcher(" server ", " stat ") calling:" watch-key)
      (handler new-stat))))))
     

; (defn server-id-table-watcher
;  [_ atom old new]
;  (let [new-server-ids (:new-relic-server-ids new)
;        old-server-ids (:new-relic-server-ids old)
;        new-server-hosts (into #{} (map :private-dns-name (:servers new)))]
;    (when (not (= new-server-ids old-server-ids))
;      (log "new-relic/watcher: detected update to :new-relic-server-ids")
;      (log "new-relic/watcher: new-server-hosts" new-server-hosts)        
;      (log "new-relic/watcher: new-server-ids" new-server-ids)        
;      (doseq [[host id] new-server-ids]
;        (if (contains? new-server-hosts host)
;          (update-server-stats! host id)
;         (log "new-relic/watcher: skipping server" host id))))))