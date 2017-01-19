(ns shboard.apis.aws
 (:require [cljs.nodejs :refer [require]])
 (:use [clojure.string :only [replace-first]]))

(def js-AWS (require "aws-sdk"))

(def default-url-for-instance
 "https://console.aws.amazon.com/ec2/v2/home?region=us-east-1#Instances:instanceId=~a")

(defn set-region!
 [region]
 (set! (.-region (.-config js-AWS)) region))

; (defn- find-tag
;   "Given an AWS taglist and a key, returns the value for the tag with that key, if any."
;   [taglist key]
;   (get (first (filter #(= (get % "Key") key) taglist)) "Value" "No Name"))

(defn- taglist->map
 "Converts an AWS-style taglist into a normal hash-map.
  Note that the keys are strings, not keywords."
 [taglist]
 (->> taglist (map (fn [x] [(get x "Key") (get x "Value")])) (into {})))

(defn- parse-instance-data
 [data]
 (let [tags (taglist->map (get data "Tags"))]
  {:id (get data "InstanceId")
   :name (get tags "Name")
   :state (keyword (get-in data ["State" "Name"] "none"))
   :launch-time (get data "LaunchTime")
   :private-dns-name (replace-first (get data "PrivateDnsName") ".ec2.internal" "")
   :private-ip (get data "PrivateIpAddress")
   :public-dns-name (get data "PublicDnsName")
   :public-ip (get data "PublicIpAddress")
   :tags tags}))

(defn describe-instances
 "Does an AWS describeInstances call and normalizes the response to a Clojure-friendly data model.
  AWS Docs: http://docs.aws.amazon.com/AWSJavaScriptSDK/latest/AWS/EC2.html#describeInstances-property"
 [opts callback]
 (.describeInstances (new js-AWS.EC2) (clj->js opts)
  (fn [err resp]
   (if (some? err) (throw err)
    (let [instances (js->clj (for [reserv (.-Reservations resp) inst (.-Instances reserv)] inst))
          parsed-instances (map parse-instance-data instances)]
     (println "aws/describe-instances:" "got" (map :id parsed-instances))
     (callback parsed-instances))))))
