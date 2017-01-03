(ns shboard.blessed
  (:require [shboard.util :refer [assoc-opts]]))

(def blessed (js/require "blessed"))
(def js-sparkline (js/require "sparkline"))

(defn- add-keys
	"Takes a vector of shape [[keyboard-keys] handler-fn] and iteratively adds the 
	keybindings to given Blessed node. Returns the node."
	[node keymap]
	(doseq [[keylist handler] keymap]
		(.key node (clj->js keylist) handler))
	node)

(defn add-child
  "Takes two blessed nodes, and appends the latter to the children of the former. Returns the parent node."
  [parent child]
  (.append parent child)
  parent)

(defn add-children
	"Takes a Blessed node and a list of new child nodes to add to it. Returns the parent node."
	[parent child-nodes]
	(doseq [child child-nodes]
		(add-child parent child))
  parent)

(defn blessed-node
  "Creates and returns a Blessed node"
  ([opt & opts] (blessed-node (assoc-opts opt opts)))
  ([{:keys [type options keymap children style padding]}]
    (let [node-options (merge options {:style style :padding padding})]
      (doto
        ((aget blessed type) (clj->js node-options))
        (add-keys keymap)
        (add-children children)))))

(defn screen
	"Returns a Blessed Screen object created with provided options"
  ([opt & opts] (screen (assoc-opts opt opts)))
	([opts] (-> opts
              (assoc :type "screen")
              (assoc-in [:options :smartCSR] true)
              (blessed-node))))

(defn layout
  "Returns a Blessed Layout object created with provided options."
  ([opt & opts] (layout (assoc-opts opt opts)))
	([opts] (-> opts
              (assoc :type "layout")
              (blessed-node))))

(defn box
  "Returns a Blessed Box object created with provided options."
  ([opt & opts] (box (assoc-opts opt opts)))
	([opts] (-> opts
              (assoc :type "box")
              (blessed-node))))

(defn text
  "Returns a Blessed Text object created with provided options."
  ([content] (text content {}))
  ([content opt & opts] (text content (assoc-opts opt opts)))
	([content opts] (-> opts
                      (assoc :type "text")
                      (assoc-in [:options :content] content)
                      (blessed-node))))

(defn sparkline
  "Returns a Blessed Text object containing a sparkline for given data."
  ([data] (sparkline data {}))
  ([data opt & opts] (sparkline data (assoc-opts opt opts)))
  ([data opts] (-> opts
                   (assoc :type "text")
                   (assoc-in [:options :content] (if (nil? data) "No data" (js-sparkline (clj->js data))))
                   (blessed-node))))