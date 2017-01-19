(ns shboard.ui.dashboard
 (:require [shboard.apis.blessed :as blessed :refer [text
                                                     layout
                                                     box
                                                     generate-tags]]
           [shboard.apis.node.process :refer [exit]]
           [shboard.apis.sparkline :refer [sparkline-from-seq]]
           [shboard.apis.opn :refer [opn]]
           [shboard.apis.clipboard :as clipboard]
           [shboard.ui.helpers :refer [readable-bytes
                                       readable-percent
                                       render-into!]]
           [shboard.ui.state :refer [get-state-value
                                     set-state-value!
                                     request-render!]]
           [shboard.util :refer [merge-deep
                                 in-range?
                                 kw->str]]
           [shboard.servers :as servers]
           [shboard.config :refer [get-config-value]]
           [shboard.colors :refer [string->color
                                   get-style]]
           [shboard.server-stats :as server-stats]
           [shboard.scroll :as scroll]
           [cljs.pprint :refer [cl-format]])
 (:use [clojure.string :only [join]]))

(defn style-for-state
 [state]
 (case state
   (:terminated :missing) (get-style :error)
   (:shutting-down :stopped :stopping) (get-style :warning)
   (get-style :active)))

(defn row-text
 "Defines an element in a dashboard row. Basically, just a text node with some default settings."
 [content opts]
 (blessed/text content (merge-deep {:padding {:left 1 :right 1}} opts)))


(defn stat-sparkline
 "Creates a sparkline that is linked to given server and stat.
  Note that only one stat-sparkline can be active for any given server/stat combo."
 [server-id stat-key {:keys [label pprinter disabled-style]
                      :defaults {:label "" :pprinter str :disabled-style ""}
                      :as opts}]
 (let [watch-key     [:stat-sparkline server-id stat-key]
       current-stats (server-stats/get-stat-for-server server-id stat-key)
       stats2content
        (fn [stats]
         (str label
          (if (nil? stats) (generate-tags disabled-style "No data")
                           (cl-format nil "~a ~10a" (pprinter (last stats))
                                                    (sparkline-from-seq (take-last 10 stats))))))
       element       (row-text (stats2content current-stats)
                               (merge-deep {:options {:tags true}
                                            :width 20
                                            :padding {:left 1 :right 1}}
                                           opts))
       update-sparkline #(render-into! element (stats2content %))]
  (server-stats/add-stat-watcher server-id stat-key watch-key update-sparkline)
  element))

(defn server-row
 "Returns an object representing the UI element for a server row in the dashboard.
  Expects to be vertically positioned by its caller using the layout-opts arguments.
  Usage: (server-row server-data server-stats :top 5)"
 [{:keys [shboard.servers/id
          shboard.servers/name
          shboard.servers/class
          shboard.servers/subclass
          shboard.servers/state
          shboard.servers/tags
          shboard.servers/launch-time
          shboard.servers/private-ip
          shboard.servers/private-dns-name
          shboard.servers/public-dns-name
          shboard.servers/public-ip] :as server}
  {:keys [metrics-enabled?] :as opts}]
 (let [; Row state
       selected?  (= id (get-state-value [:dashboard :selected]))
       ; Event handlers
       select-me (fn [& _]
                  (set-state-value! [:dashboard :selected] id true))
       open-me   #(opn (cl-format nil 
                                  (get-config-value [:aws :url-for-instance] "")
                                  id))
       ; Colors
       active-style  (get-style (if selected? :focused :active))
       muted-style   (merge (get-style :muted) {:bg (:bg active-style)})
       name-style    (into active-style {:fg (string->color name) :bold true})
       id-style      (merge (style-for-state state) {:bg (:bg active-style)})
       ; Child elements
       cpu-sparkline    (stat-sparkline id :shboard.server-stats/cpu    {:label "C "
                                                                         :pprinter readable-percent
                                                                         :disabled-style muted-style
                                                                         :style active-style})
       memory-sparkline (stat-sparkline id :shboard.server-stats/memory {:label "M "
                                                                         :pprinter readable-bytes  
                                                                         :disabled-style muted-style
                                                                         :style active-style})
       disk-sparkline   (stat-sparkline id :shboard.server-stats/disk   {:label "D "
                                                                         :pprinter readable-percent
                                                                         :disabled-style muted-style
                                                                         :style active-style})
       net-sparkline    (stat-sparkline id :shboard.server-stats/net    {:label "N "
                                                                         :pprinter readable-bytes  
                                                                         :disabled-style muted-style
                                                                         :style active-style})
       child-nodes      (cond-> []
                         true (into [(row-text id {:width 9
                                                   :hoverText (cl-format nil "~a\nState: ~:(~a~)\n(click to copy ID)" id (kw->str state))
                                                   :style id-style
                                                   :click-el #(clipboard/write id)})
                                     (row-text name {:width 35
                                                     :hoverText (cl-format nil "~:{~35a: ~35a\n~}" tags)
                                                     :style name-style})
                                     (row-text private-ip {:style active-style
                                                           :width 17
                                                           :hoverText (str private-dns-name "\n(click to copy IP)")
                                                           :click-el #(clipboard/write private-ip)})
                                     (row-text public-ip {:style active-style
                                                          :width 17
                                                          :hoverText (str public-dns-name "\n(click to copy IP)")
                                                          :click-el #(clipboard/write public-ip)})])
                         metrics-enabled?       (into [cpu-sparkline memory-sparkline disk-sparkline net-sparkline])
                         (not metrics-enabled?) (into [(row-text "No metrics enabled." {})]))]
        ; [(row-text id :width 9
        ;               :options {:hoverText id}
        ;               :style {:fg id-color
        ;                       :bg default-bg})
        ;  (row-text name :width 35
        ;                 :style {:fg name-color
        ;                         :bg default-bg})
        ;  (row-text private-ip :style default-style
        ;                       :width 17)
        ;  (row-text public-ip :style default-style
        ;                      :width 17)
        ;  cpu-sparkline memory-sparkline disk-sparkline net-sparkline]]
  (layout (merge-deep {:children child-nodes
                       :click-el (if selected? open-me select-me)
                       :height 1
                       :width "100%"}
                      opts))))

(defn dashboard-layout
 "Returns a Blessed object for a dashboard with given parameters"
 [opts]
 (let [scroll-offset (scroll/get-offset :dashboard)
       metrics-enabled? (not (get-config-value [:flags :no-metrics]))
       servers (->> (servers/get-all-servers) 
                    (sort-by :shboard.servers/name)
                    (drop scroll-offset)
                    (into []))
       select-server (fn [offset]
                      (let [selected-id (get-state-value [:dashboard :selected])
                            selected-server (first (filter #(= (:shboard.servers/id %) selected-id) servers))
                            selected-ix (.indexOf servers selected-server)
                            new-ix (+ offset selected-ix)
                            new-server (get servers new-ix)
                            new-id (:shboard.servers/id new-server)]
                       (set-state-value! [:dashboard :selected] new-id true)))
       dashboard-rows (map #(server-row %1 {:top %2 :metrics-enabled? metrics-enabled?})
                           servers
                           (range))
       loading-text (row-text "Loading server list..."
                              {:width "100%"})
       children (if (= (count servers) 0) 
                    loading-text
                    dashboard-rows)
       box-opts (merge-deep {:options {:scrollable true}
                             :children dashboard-rows
                             :keymap-el {["q"] #(exit 0)
                                         ["R"] servers/update-server-data!
                                         ["r"] server-stats/update-all-stats!
                                         ["down"] #(select-server 1)
                                         ["up"] #(select-server -1)}}
                            opts)
       container (box box-opts)
       scroll #(scroll/add-to-offset! :dashboard % (- (count servers) (.-height container)))]
  (scroll/add-scroll-watch :dashboard :dashboard-render request-render!)
  (doto container
   (.on "element wheelup" #(scroll -2))
   (.on "element wheeldown" #(scroll 2))
   (.focus))))