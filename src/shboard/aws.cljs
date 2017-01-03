(ns shboard.aws
  (:require [shboard.util :refer [debug-log]]))

(def AWS (js/require "aws-sdk"))

(set! (.-region (.-config AWS)) "us-east-1")

(def class-for-name {
  "REDOX CI Build Manager" :build
  "REDOX Container HAProxy Cluster Primary" :haproxy
  "REDOX Container HAProxy Cluster Member" :haproxy
  "REDOX Elasticsearch Cluster Node" :elasticsearch
  "REDOX Kibana Server Host" :kibana
  "REDOX Postgres Cluster Master" :postgresql
  "REDOX Postgres Cluster Slave" :postgresql
  "REDOX Rancher Management Cluster" :ranchermgmt
  "REDOX Rancher Container Host" :docker
  "REDOX REDIS Cluster Master" :redis
  "REDOX REDIS Cluster Slave" :redis
  "REDOX VPN Primary" :vpn
  "REDOX VPN Cluster Member" :vpn
})

(defn- find-tag
  "Given an AWS taglist and a key, returns the value for the tag with that key, if any."
  [taglist key]
  (get (first (filter #(= (get % "Key") key) taglist)) "Value" "No Name"))

(defn- find-subclass
  "Given an instance data object and a tag, returns any sub-tags (e.g. :master, :slave, etc.)"
  [instance-class instance-data]
  (let [tags (instance-data "Tags")]
    (case instance-class
      :postgresql (if (= (find-tag tags "pgreplicationrole") "master") :master :slave)
      :redis      (if (= (find-tag tags "redisrole") "master") :master :slave)
      :vpn        (if (= (find-tag tags "vpnrole") "primary") :master :slave)
      :haproxy    (if (= (find-tag tags "haproxyrole") "primary") :master :slave)
      :none)))

(defn fetch-server-data!
  "Asynchronously loads a flat list of EC2s into the provided atom. See AWS SDK docs for response format:
   http://docs.aws.amazon.com/AWSJavaScriptSDK/latest/AWS/EC2.html#describeInstances-property"
  [target-atom target-path]
  (let [parse-instance-data
          (fn [data]
            (let [instance-name (find-tag (get data "Tags") "Name")
                  instance-class (get class-for-name instance-name :none)
                  instance-subclass #({})]
              {:id (get data "InstanceId")
               :name instance-name
               :class instance-class
               :subclass (find-subclass instance-class data)
               :state (keyword (get-in data ["State" "Name"] "missing"))
               :launch-time (get data "LaunchTime")
               :private-dns-name (get data "PrivateDnsName")
               :private-ip (get data "PrivateIpAddress")
               :public-dns-name (get data "PublicDnsName")
               :public-ip (get data "PublicIpAddress")}))
        callback
          (fn [err resp]
            (if (not (nil? err)) (throw err)
              (let [instances (js->clj (for [reserv (.-Reservations resp) inst (.-Instances reserv)] inst))
                    parsed-instances (map parse-instance-data instances)]
                (debug-log "fetch-server-data" "callback" (first parsed-instances))
                (swap! target-atom assoc-in target-path parsed-instances))))]
    (.describeInstances (new AWS.EC2) #js{} callback)))