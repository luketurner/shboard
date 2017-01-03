(ns shboard.ui
  (:require [shboard.blessed :as blesssed :refer [text add-children add-child layout sparkline box]]
            [shboard.util :refer [assoc-opts]]
            [shboard.state :as state]))

(def color-for-class {
  :postgresql "#cb4b16" ;orange
  :redis "#b58900" ;yellow
  :elasticsearch "#d33682" ;magenta
  :kibana "#6c71c4" ;violet
  :haproxy "#839496" ;brblue
  :vpn "#268bd2" ;blue
  :build "#859900" ;green
  :docker "#2aa198" ;cyan
  :ranchermgmt "#93a1a1" ;brcyan
  :none "fg"
})

(def abbrev-for-class {
  :postgresql "PG"
  :redis "RE"
  :elasticsearch "ES"
  :kibana "KI"
  :haproxy "HA"
  :vpn "VP"
  :build "CI"
  :docker "DO"
  :ranchermgmt "RN"
  :none "N/A"
})

(def abbrev-for-subclass {
  :master "M"
  :slave "S"
  :none ""
})

(def color-for-state {
  :terminated "#dc322f" ;red
  :shutting-down "#cb4b16" ;orange
  :stopped "#cb4b16" ;orange
  :stopping "#cb4b16" ;orange
  :missing "#dc322f" ;red
  :running "bg"
})

(defn server-row
  "Returns an object representing the UI element for a server row in the dashboard"
  ([row-index {{cpu-stats :cpu mem-stats :memory disk-stats :disk net-stats :net :defaults {cpu-stats [] mem-stats [] disk-stats [] net-stats []} } :stats
     :keys [id name class subclass state launch-time private-ip public-ip]
     :as opts}]
    (let [abbrev (str (abbrev-for-class class) (abbrev-for-subclass subclass))
          class-color (color-for-class class)
          id-color (color-for-state state)
          child-nodes
            [(text abbrev :style {:bg class-color} :padding {:left 1 :right 1} :options {:width 5 :hoverText name})
             (text id :style {:bg id-color} :padding {:left 1 :right 1} :options {:width 21})
             (text private-ip :padding {:left 1 :right 1} :options {:width 17})
             (text public-ip :padding {:left 1 :right 1} :options {:width 17}) 
             (text "C" :padding {:left 2 :right 1}) (sparkline cpu-stats :options {:width 10})
             (text "M" :padding {:left 2 :right 1}) (sparkline mem-stats :options {:width 10})
             (text "D" :padding {:left 2 :right 1}) (sparkline disk-stats :options {:width 10})
             (text "N" :padding {:left 2 :right 1}) (sparkline net-stats :options {:width 10})]]
      (-> opts
        (assoc :children child-nodes)
        (assoc-in [:options :height] 1)
        (assoc-in [:options :width] "100%")
        (assoc-in [:options :top] row-index)
        (assoc-in [:options :scrollable] true)
        (layout)))))

()

(defn dashboard
  "Returns a fully-described dashboard object"
  ([opt & opts] (dashboard (assoc-opts opt opts)))
  ([opts]
    (let [servers (:servers @state/db)
          num-servers (count servers)
          scroll-dashboard (fn [offset]
                            (swap! state/db update-in [:scroll :dashboard]
                              #(min 0 (+ % offset))))
          scroll-offset (get-in @state/db [:scroll :dashboard] 0)
          dashboard-rows (map-indexed #(server-row (+ scroll-offset %1) %2) servers)
          dashboard-box (-> opts 
                            (assoc :children dashboard-rows)
                            (assoc-in [:options :scrollable] true)
                            (box))]
        (.on dashboard-box "wheelup" #(scroll-dashboard 2))
        (.on dashboard-box "wheeldown" #(scroll-dashboard -2))
        dashboard-box)))