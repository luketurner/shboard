(ns shboard.apis.node.process)

(defn env-var
 "Looks up an environment variable in process.env"
 [key]
 (-> js/process (aget "env") 
                (aget key)))


(defn exit
 "Quits the program with given exit code"
 [code]
 (.exit js/process code))