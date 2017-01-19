(ns shboard.ui
 (:require [shboard.apis.blessed :as blessed :refer [text
                                                     layout
                                                     box]]
           [shboard.apis.new-relic :as new-relic]
           [shboard.apis.node.process :refer [exit]]
           [shboard.apis.sparkline :refer [sparkline-from-seq]]
           [shboard.apis.opn :refer [opn]]
           [shboard.ui.dashboard :refer [dashboard-layout]]
           [shboard.ui.header :refer [header-layout header-height]]
           [shboard.ui.screen :refer [screen
                                      create-screen!]]
           [shboard.util :refer [merge-deep
                                 in-range?
                                 kw->str]]
           [shboard.servers :as servers]
           [shboard.config :refer [get-config-value]]
           [shboard.colors :refer [string->color
                                   get-style]]
           [shboard.server-stats :as server-stats]
           [shboard.scroll :as scroll]
           [shboard.ui.state :refer [get-state-value
                                     set-state-value!
                                     add-render-watch]]
           [cljs.nodejs :refer [require]]
           [cljs.pprint :refer [cl-format]])
 (:use [clojure.string :only [join]]))

; (defn start-render-loop!
;  "Starts a loop that re-renders the app if a render is requested."
;  []
;  (let [maybe-render? #(when @render-requested?
;                        (println "rendering...")
;                        (reset! render-requested? false)
;                        (.render @screen))]
;   (js/setInterval maybe-render? 200)))

; (def request-render!
;  "Requests a re-render when next available."
;  (_throttle #(.render @screen) 50))


; root-layout depends on render, and render calls root-layout.
; therefore, we declare layout early so it can be used in render.
(declare root-layout)

(defn render
 "High-level function to re-reate and re-render the entire UI.
  Should be called whenever UI state changes."
 []
 (let [screen    @screen
       old-child (-> screen (.-children) (aget 0))
       new-child (root-layout {:width  "100%"
                               :height "100%"})]
  (if (some? old-child) (.destroy old-child))
  (blessed/add-child! screen new-child)
  (.render screen)))

; (defn render-into
;  "Sets the content of given element and re-renders to update the screen immediately.
;   Because renders are throttled, this can be called repeatedly without causing lag...
;   hopefully."
;  [target-element new-content]
;  (.setContent target-element new-content)
;  (request-render!))

(defn root-layout
 "Returns a fully-described dashboard object"
 [opts]
 (let [header (header-layout {:style (get-style :inactive)})
       content (dashboard-layout {:width "100%"
                                  :top header-height
                                  :style (get-style :active)})
       root (layout (merge-deep {:children [header content]
                                 :focused true
                                 :width "100%"
                                 :height "100%"}
                                opts))]
  (.focus content)
  root))

(defn initialize-ui
 "Initializes new UI, wires up application state, and launches async data retrievers."
 [{{:keys [no-metrics]} :flags}]
 (add-render-watch :ui/render render)
 (servers/add-update-watch :ui/render render)
 (servers/add-insert-watch :server-stats/insert-server-watcher new-relic/update-server-ids!)
 (add-watch new-relic/server-id-table :stats-poll 
  (fn [_ _ o n]
   (when (and (not= o n) 
              (< 0 (count n)))
    (server-stats/update-all-stats!))))
 (servers/update-server-data!)
 (create-screen!)
 (render))
