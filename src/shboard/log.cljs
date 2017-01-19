(ns shboard.log
 (:require [cljs.nodejs :refer [require]]
           [clojure.string :as string]
           [cljs.core.async :refer [<! promise-chan put!]])
 (:require-macros [cljs.core.async.macros :refer [go]]))

(def fs (require "fs"))

; (def log-data (atom {}))

; (defn log-line!
;  "Logs some data to the log data"
;  [dest & xs]
;  (swap! log-data update-in [dest :lines] #(conj % {:date (new js/Date)
;                                                    :text (join " " xs)})))

(def logging-to-file false)

(defn log
 [& args]
 (if logging-to-file
  (apply println args)))


; TODO - is it a problem to do this synchronously?
(defn- append-to-file
 [file & args]
 (.appendFileSync fs file (string/join " " args)))

(defn start-logging-to-file!
 [file]
 (go 
  (let [logger        (partial append-to-file file)
        trunc-promise (promise-chan)]
   (.writeFile fs file "" #(if %1 (throw %1) (put! trunc-promise true)))
   (<! trunc-promise)
   (set! *print-newline* true)
   (set! *print-fn* logger)
   (set! *print-err-fn* logger)
   (set! logging-to-file true))))