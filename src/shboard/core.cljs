(ns shboard.core
  (:require [cljs.nodejs :as nodejs]
            [cljs.tools.cli :refer [parse-opts]]
            [shboard.blessed :as blessed]
            [shboard.aws :as aws]
            [shboard.new-relic :as new-relic]
            [shboard.ui :as ui]
            [shboard.state :as state]))

(nodejs/enable-util-print!)

(defn env-var [key] (-> js/process (aget "env") (aget key)))

(defn exit
  "Quits the program with given exit code"
  [code]
  (.exit js/process code))

(def cli-options
  [["-h" "--help" "Display this help message"]
   [nil, "--debug", "Display debug messages"]
   ["-N" "--new-relic-key KEY" "New Relic API key to use"
    :default (env-var "NEW_RELIC_API_KEY")
    :default-desc "NEW_RELIC_API_KEY"]
   ["-P" "--aws-profile PROFILE" "AWS profile to use"
    :default (env-var "AWS_PROFILE")
    :default-desc "AWS_PROFILE"]])


(defn initialize-dashbord
  "Creates a dashboard screen, wires up applicatino state, and launches async data retrievers."
  [{:keys [new-relic-key]}]
  (let [screen (blessed/screen :title "shboard"
                               :keymap [[["q" "escape" "C-c"] #(exit 0)]])
        add-dashboard #(blessed/add-child screen (ui/dashboard :options {:width "100%" :height "100%"}))
        render #(do (add-dashboard) (.render screen))
        update-new-relic (fn [_ _ old new]
                           (let [old-servers (:servers old)
                                 new-servers (:servers new)]
                             (if (not (= old-servers new-servers))
                               (new-relic/fetch-server-ids! (map :private-dns-name new-servers)))))]
    (println new-relic-key)
    (println (swap! state/db assoc-in [:config :new-relic-api-key] new-relic-key))
    (println (get-in @state/db [:config :new-relic-api-key]))
    (add-watch state/db nil render)
    (add-watch state/db nil update-new-relic)
    (aws/fetch-server-data! state/db [:servers])
    (render)))

(defn -main [& args]
  (let
    [{{help?  :help
       debug? :debug
       :as options} :options
      help-summary :summary}
     (parse-opts args cli-options)]
    (when debug?
      (state/add-debug-watcher!)
      (swap! state/db assoc :debug true))
    (println "options" options)
    (if help?
      (println help-summary)
      (initialize-dashbord options))))

(set! *main-cli-fn* -main)