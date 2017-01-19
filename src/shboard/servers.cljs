(ns shboard.servers
 (:require [cljs.spec :as spec]
           [shboard.util :refer [same-values?
                                 keyset
                                 explain-valid?]]
           [shboard.apis.aws :as aws]
           [clojure.set :as set]
           [shboard.log :refer [log]]))

(spec/def ::id string?)
(spec/def ::name string?)
(spec/def ::class keyword?)
(spec/def ::subclsass keyword?)
(spec/def ::state #{:none :running :stopped :stopping :terminated :shutting-down})
(spec/def ::launch-time some?) ; TODO - what should this be?
(spec/def ::private-dns-name string?)
(spec/def ::private-ip string?)
(spec/def ::public-dns-name string?)
(spec/def ::public-ip string?)
(spec/def ::tags (spec/every-kv string? string?))

(spec/def ::server (spec/keys :req [::id
                                    ::class
                                    ::subclass
                                    ::state
                                    ::launch-time
                                    ::tags]
                              :opt [::name
                                    ::public-dns-name
                                    ::public-ip
                                    ::private-dns-name
                                    ::private-ip]))
                                   
                                   
                        
(spec/def ::server-index (spec/map-of ::id ::server))

(def default-server-index
 {})

(def server-index
 "Stores static (non-statistical) information about servers. This information is probably mostly coming from AWS.
  Servers are stored in a hash-map based on their ID."
 (atom default-server-index :validator #(explain-valid? ::server-index %)))

(defn aws-instance->server
 [instance]
 (->> {::id (:id instance)
       ::name (get instance :name "")
       ::class :none
       ::subclass :none
       ::state (:state instance)
       ::launch-time (:launch-time instance)
       ::private-dns-name (:private-dns-name instance)
       ::private-ip (:private-ip instance)
       ::public-dns-name (:public-dns-name instance)
       ::public-ip (:public-ip instance)
       ::tags (:tags instance)}
       (filter (comp some? second))
       (into {})))
       

(defn put-servers!
 "Accepts a seq of servers and adds them to the index."
 [new-servers]
 (->> new-servers
  (map (fn [x] [(::id x) x]))
  (into {})
  (swap! server-index merge)))

(defn update-server-data!
  "Asynchronously loads server data into the server index."
  []
  (aws/describe-instances {} #(->> % (map aws-instance->server) (put-servers!))))

(defn get-server
 "Gets information hash for a single server based on id"
 [id]
 (get @server-index id))


(defn get-all-servers
 "Returns a seq of all servers."
 []
 (vals @server-index))

(defn add-update-watch
 "Creates a watcher that's called whenever the index is updated in any way."
 [key handler]
 (add-watch server-index key
  (fn [k a o n]
      (when (not= o n)
       (log "servers/add-update-watch calling:" key)
       (handler n))))) 

(defn add-insert-watch
 "Creates a watcher that's called whenever a new server is inserted into the index.
  Handler function is called with a seq of the added servers."
 [key handler]
 (add-watch server-index key
  (fn [k a old new]
      (when-let [new-ids (set/difference (keyset new) (keyset old))]
       (log "servers/add-insert-watch calling:" key)
       (handler (map new new-ids)))))) 

(defn add-remove-watch
 "Creates a watcher that's called whenever a server is removed from the index.
  Handler function is called with a seq of the removed servers."
 [key handler]
 (add-watch server-index key
  (fn [k a old new]
      (when-let [removed-ids (set/difference (keyset old) (keyset new))]
       (log "servers/add-remove-watch calling:" key)
       (handler (map old removed-ids))))))