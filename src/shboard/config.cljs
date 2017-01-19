(ns shboard.config
 (:require [cljs.tools.cli :as cli]
           [clojure.string :as string]
           [shboard.util :refer [explain-valid?
                                 same-values-in?]]
           [shboard.apis.blessed :refer [color?]]
           [shboard.apis.node.fs :refer [read-file]]
           [shboard.apis.node.process :refer [env-var]]
           [shboard.apis.aws :refer [default-url-for-instance]]
           [cljs.spec :as spec]
           [clojure.core.async :refer [<!]]
           [cljs.reader :as reader]
           [cljs.pprint :refer [pprint]])
 (:require-macros [cljs.core.async.macros :refer [go]]))

(spec/def ::log-file (spec/or :log-file string? :log-file nil?)) ; TODO
(spec/def ::config-file string?)
(spec/def ::aws-region (spec/or :aws-region string? :aws-region nil?)) ; TODO
(spec/def ::aws-profile string?)

; Color-related definitions

(spec/def ::fg color?)
(spec/def ::bg color?)

(spec/def ::style
 (spec/keys :req-un [::fg
                     ::bg]))

(spec/def ::active   ::style)
(spec/def ::inactive ::style)
(spec/def ::focused  ::style)
(spec/def ::warning  ::style)
(spec/def ::error    ::style)
(spec/def ::muted    ::style)
(spec/def ::color-wheel (spec/or :color-wheel #{:automatic}
                                 :color-wheel vector?))

(spec/def ::color-scheme
 (spec/keys :req-un [::active
                     ::inactive
                     ::focused
                     ::warning
                     ::error
                     ::muted
                     ::color-wheel ;used when you want a choice/cycle of colors (e.g. server names).
                     ]))

; map of API keys (these are considered quasi-secret)
(spec/def ::api-keys
 (spec/keys ::opt-un [::new-relic
                      ::papertrail]))

; map of all the boolean flags from the CLI
(spec/def ::debug boolean?)

(spec/def ::flags
 (spec/keys ::req-un [::debug
                      ::help
                      ::print-config
                      ::save-config
                      ::save-api-keys
                      ::run-tests
                      ::no-metrics]))

; map of all the AWS stuff.
(spec/def ::aws
 (spec/keys :req-un [::profile
                     ::region
                     ::url-for-instance]))

; spec for global app config atom
(spec/def ::config
 (spec/keys :req-un [::color-scheme
                     ::config-file
                     ::flags
                     ::api-keys
                     ::aws]
            :opt-un [::log-file]))


(def current-config
 "Contains the app's current configuration."
 (atom {} :validator #(explain-valid? ::config %)))

(def cli-parameters
 [; Boolean flags
  ["-h" "--help"          "Display this help message and exit."]
  [nil  "--debug"         "Enable debugging instrumentation (may impact performance)."]
  [nil  "--run-tests"     "Run test.check suite instead of launching shboard."]
  [nil  "--print-config"  "Print config (including API keys) to STDOUT and exit."]
  ["-S" "--save-config"   "Write current config (except API keys) to config file."]
  [nil  "--save-api-keys" "Write API keys to config file. Implies --save-config."]
  ["-M" "--no-metrics"    "Disable New Relic metrics."]

  ; General
  ["-l" "--log-file FILE" "Output diagnostic logging into specified log file."]
  ["-c" "--config-file FILE" "Override config file location"
   :default-desc "$XDG_CONFIG_HOME/shboard/config.edn"
   :default (str (or (env-var "XDG_CONFIG_HOME") "~/.config")
                 "/shboard/config.edn")]

  ; AWS
  ["-P" "--aws-profile PROFILE" "AWS profile to use. If none is specified, will use \"default\""
   :default-desc "$AWS_PROFILE"
   :default      (env-var "AWS_PROFILE")]
  ["-r" "--aws-region REGION" "AWS region to use. Overrides profile default."
   :default-desc "$AWS_REGION"
   :default      (env-var "AWS_REGION")]

  ; New Relic
  [nil "--new-relic-key KEY" "New Relic API key to use. Required for server metrics."
   :default-desc "$NEW_RELIC_API_KEY"
   :default (env-var "NEW_RELIC_API_KEY")]
  ; [nil "--poll-metrics SECONDS" "Polls New Relic metrics every SECONDS seconds. Default is no automatic polling."]

  ; Papertrail
  [nil "--papertrail-key KEY" "API key for Papertrail. Required for viewing logs."
   :default-desc "$PAPERTRAIL_API_KEY"
   :default (env-var "PAPERTRAIL_API_KEY")]])


; interesting greyscale color wheel:
; ["#ffffff" "#eeeeee" "#dddddd" "#cccccc" "#bbbbbb" "#aaaaaa" "#999999"]

; default color scheme based on Solarized: http://ethanschoonover.com/solarized
(def default-colors 
 {:active   {:bg "#073642"
             :fg "#aaaaaa"}
  :inactive {:bg "#002b36"
             :fg "#93a1a1"}
  :focused  {:bg "#586e75"
             :fg "#fdf6e3"}
  :warning  {:bg "#073642"
             :fg "#cb4b16"}
  :error    {:bg "#073642"
             :fg "#b58900"}
  :muted    {:bg "#073642"
             :fg "#657b83"}
  :color-wheel ["#b58900", "#cb4b16", "#dc322f", "#d33682", "#6c71c4", "#268bd2", "#2aa198", "#859900"]})

(def default-console-url)

(defn help-text
 "Returns a helpful string to print if the user asks for it"
 [option-summary]
 (->> [""
       "Usage: shboard [options]"
       ""
       "Options:"
       option-summary
       ""]
  (string/join \newline)))

(defn error-text
 "Returns descriptive text for any configuration parsing errors"
 [errors]
 (str "Errors encountered while parsing shboard configuration:"
  \newline
  (string/join \newline errors)))

(defn get-cli-output
 "Returns any CLI output as requested by the CLI flags (e.g. --help or --print-config).
  If existing, the CLI output should be printed on STDOUT in lieu of displaying the screen.
  If it returns nil, no output was requested, and the screen can be painted."
 [raw-cli-args]
 (let [{{:keys [help print-config]} :options :keys [errors summary]} (cli/parse-opts raw-cli-args cli-parameters)]
  (cond
   errors {:text (str (error-text errors)
                      \newline
                      (help-text summary))
           :exit-code 1}
   help {:text (str "\n// shboard // :: a devops dashboard in your terminal window\n"
                    (help-text summary))
         :exit-code 0}
   print-config {:text (with-out-str (pprint (into {} (filter (comp not #{:flags} first) @current-config))))
                 :exit-code 0})))

(defn load-config
 "Loads a configuration hash based on the current execution environment.
  Configuration variables are loaded from the following places, in order of priority:
   1. Command-line arguments
   2. Environment variables (for API keys in particular)
   3. A config file at ~/.config/shboard/config.edn (NOT IMPLEMENTED)
  Returns a promise channel with the new config values."
 [raw-cli-args]
 (go (let [{{:keys [config-file] :as cli-opts} :options
            cli-errors :errors} (cli/parse-opts raw-cli-args cli-parameters)
       {:as saved-opts}  (-> config-file
                            (read-file)
                            (<!)
                            (or "")
                            (reader/read-string))]
  (if cli-errors nil
   {:api-keys {:new-relic  (or (:new-relic-key cli-opts)
                               (get-in [:api-keys :new-relic] saved-opts))
               :papertrail (or (:papertrail-key cli-opts)
                               (get-in [:api-keys :papertrail] saved-opts))}
    :aws {:profile (or (get-in cli-opts [:aws-profile])
                       (get-in saved-opts [:aws :profile])
                       "default")
          :region (or (get-in cli-opts [:aws-region])
                      (get-in saved-opts [:aws :region]))
          :url-for-instance (or (get-in saved-opts [:aws :url-for-instance])
                                default-url-for-instance)}
    :color-scheme (or (:color-scheme saved-opts)
                      default-colors)
    :config-file  config-file
    :log-file     (or (:log-file cli-opts)
                      (:log-file saved-opts))
    :flags        (->> cli-opts
                       (filter (fn [[k _]] (#{:run-tests :debug :help :print-config :save-config :save-api-keys :no-metrics} k)))
                       (map (fn [[k v]] [k (boolean v)]))
                       (into {}))}))))

(defn update-config!
 "Updates the current configuration using load-config.
  Returns a promise channel with the new config values.
  Note that global config may not be consistent until the promsie is resolved."
 [raw-cli-args]
 (go
  (if-let [new-config (<! (load-config raw-cli-args))]
   (reset! current-config new-config))))

(defn get-config-value
 "Gets config-key (which can be a path vector or a plain key) from the config store.
  Accepts an optional default value that will be returned if the config value does not exist."
 ([config-key] ((if (vector? config-key) get-in get) @current-config config-key))
 ([config-key default] ((if (vector? config-key) get-in get) @current-config config-key default)))

; (defn add-update-watch
;  "Creates a watcher that's called whenever the config is updated in any way."
;  [key handler]
;  (add-watch current-config key
;   (fn [k a o n]
;       (when (not= o n)
;        (handler n)))))