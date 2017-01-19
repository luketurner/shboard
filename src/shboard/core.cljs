(ns shboard.core
 (:require [cljs.nodejs :as nodejs]
           [shboard.apis.blessed :as blessed]
           [shboard.apis.aws :as aws]
           [shboard.apis.new-relic :as new-relic]
           [shboard.apis.node.fs :refer [write-file mkdir]]
           [shboard.apis.node.path :refer [dirname]]
           [shboard.apis.node.process :refer [exit]]
           [shboard.config :as config]
           [shboard.ui :refer [initialize-ui]]
           [shboard.util :refer [without-keys]]
           [shboard.servers :as servers]
           [shboard.server-stats :as server-stats]
           [shboard.log :as log]
           [clojure.string :as string]
           [cljs.pprint :refer [pprint]]
           [cljs.spec.test :refer [instrument check]]
           [cljs.core.async :refer [<!]])
 (:require-macros [cljs.core.async.macros :refer [go]]))

; makes print and friends use console.log
(nodejs/enable-util-print!)

(defn disable-print!
 []
 (let [void-print (fn [& args] nil)]
  (set! *print-fn* void-print)
  (set! *print-err-fn* void-print)))

(defn -main
 [& raw-args]
 (go
  (let [{:keys [log-file config-file]
         {:keys [region profile]} :aws
         {:keys [save-config save-api-keys run-tests debug]} :flags
         :as current-config} (<! (config/update-config! raw-args))
        {:keys [text error-code]} (config/get-cli-output raw-args)]
   (when debug
    ; enable runtime type-checking for specced functions
    (instrument))
   (when text ; print any help/error text to STDOUT
    (println text)
    (exit error-code))
   (when run-tests ; TODO - This doesn't work yet.
    (println "Running test.spec suite (this may take some time)...")
    (check)
    (println "Done.")
    (exit 0))
   (when (or save-api-keys save-config) ; note -- save-api-keys implies save-config
    (println "Writing configuration to" config-file)
    (let [keys-to-filter (if save-api-keys
                              [:flags :config-file]
                              [:flags :config-file :api-keys])
           config-string (with-out-str
                          (pprint
                           (without-keys keys-to-filter current-config)))
           mkdir-err (<! (mkdir (dirname config-file)))
           write-err (<! (write-file config-file config-string))]
     (when (some? mkdir-err)
      (println "Error creating folder for config:" mkdir-err)
      (exit 1))
     (when (some? write-err)
      (println "Error writing config:" write-err)
      (exit 1))
     (exit 0)))
   (if log-file
    (do 
     (<! (log/start-logging-to-file! log-file))
     (println "// shboard diagnostic logs // ::" (.toISOString (new js/Date)))
     (println "Current configuration (sans :api-keys):")
     (pprint  (without-keys [:api-keys] current-config))
     (println "End configuration."))
    (disable-print!))
   (aws/set-region! region)
   (aws/set-profile! profile)
   (initialize-ui current-config))))
  

(set! *main-cli-fn* -main)