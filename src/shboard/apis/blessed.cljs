(ns shboard.apis.blessed
 (:require [shboard.util :refer [merge-deep]]
           [cljs.spec :as spec]
           [cljs.nodejs :refer [require]])) 

(def js-blessed (require "blessed"))

(def blessed-color-names (-> js-blessed 
                             (.-colors)
                             (.-colorNames)
                             (js->clj)
                             (into #{})))


(defn color?
 "Returns true if val is a valid Blessed color."
 [val]
 (boolean (or (blessed-color-names val)
              (re-matches #"#[\da-f]{6}" val))))

(spec/def ::color color?)

(defn generate-tags
 "Accepts a style object (e.g. {:fg asdf :bg asdf}).
  Returns a string which is tagged with given style, whose contents is the rest of the arguments."
 [style & args]
 (.generateTags js-blessed (clj->js style) (clojure.string/join " " args)))

(defn add-keys!
 "Takes a map of shape {[keyboard-keys] handler-fn} and iteratively adds the 
  keybindings to given Blessed node. Returns the node."
 [node keymap]
 (if (some? keymap) (.enableKeys node))
 (if (some? keymap) (println "add-keys!" (keys keymap)))
 (doseq [[keylist handler] keymap]
  (.on node (->> keylist (map (partial str "key ")) (clj->js)) handler))
 node)

(defn add-element-keys!
 "Takes a map of shape {[keyboard-keys] handler-fn} and iteratively adds the 
  keybindings to given Blessed node. Uses 'element' in the event. Returns the node."
 [node keymap]
 (if (some? keymap) (.enableKeys node))
 (if (some? keymap) (println "add-element-keys!" (keys keymap)))
 (doseq [[keylist handler] keymap]
  (.on node (->> keylist (map (partial str "element key ")) (clj->js)) handler))
 node)

(defn add-child!
 "Takes two blessed nodes, and appends the latter to the children of the former. Returns the parent node."
 [parent child]
 (.append parent child)
 parent)

(defn add-children!
 "Takes a Blessed node and a list of new child nodes to add to it. Returns the parent node."
 [parent child-nodes]
 (doseq [child child-nodes]
  (add-child! parent child))
 parent)

(defn add-listener!
 "Adds an event listener to a Blessed node.
  Returns the node."
  [el event-type listener]
  (if (some? listener)
   (doto el
    (.on (clj->js event-type) 
     (fn [& args]
      (apply listener (map js->clj args)))))
   el))

(defn blessed-node
 "Creates and returns a Blessed node"
 [{:keys [type options 
          children content
          click click-el
          keymap keymap-el
          style padding border
          width height
          top bottom left right
          hoverText]
          :as opts}]
 (let [node-options (merge-deep options {:content content
                                         :style style
                                         :padding padding
                                         :border border
                                         :width width :height height
                                         :top top :bottom bottom :left left :right right
                                         :hoverText hoverText})
       node ((aget js-blessed type) (clj->js node-options))]
  (when click
   (add-listener! node "click" click)
   (.enableMouse node))
  (when click-el
   (add-listener! node "element click"
    (fn [& args]
     (apply click-el args)
     false))
   (.enableMouse node))
  (doto node
   (.on "detach" #(.destroy node))
   (add-keys! keymap)
   (add-element-keys! keymap-el)
   (add-children! children))))

(defn screen
 "Returns a Blessed Screen object created with provided options"
 [opts]
 (blessed-node (merge-deep {:type "screen"} opts)))

(defn layout
 "Returns a Blessed Layout object created with provided options."
 [opts]
 (blessed-node (merge-deep {:type "layout"} opts)))

(defn box
 "Returns a Blessed Box object created with provided options."
 [opts]
 (blessed-node (merge-deep {:type "box"} opts)))

(defn text
 "Returns a Blessed Text object created with provided options."
 [content opts]
 (blessed-node (merge-deep {:type "text" :content content} opts)))