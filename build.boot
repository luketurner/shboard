(set-env!
 :source-paths    #{"src"}
 :dependencies '[[adzerk/boot-cljs          "1.7.228-2"  :scope "test"]
                 [org.clojure/clojurescript "1.9.211"]
                 [org.clojure/tools.cli "0.3.5"]
                 [org.clojure/test.check "0.9.0"]
                 [org.clojure/core.async "0.2.395"]
                 [com.rpl/specter "0.13.2"]])

(require
 '[adzerk.boot-cljs :refer [cljs]])


(deftask copy-node-modules []
 (sift :add-asset #{"node_modules"}))

(def shared-cljs-opts {:target :nodejs})

(deftask build-cljs-dev []
 (cljs :compiler-options (merge shared-cljs-opts {:source-map true
                                                  :optimizations :none})))

(deftask build-cljs-release []
 (cljs :compiler-options (merge shared-cljs-opts {:optimizations :simple})))


(deftask build []
  (comp (copy-node-modules)
        (build-cljs-release)
        (target)
        (speak)))

(deftask build-dev []
  (comp (copy-node-modules)
        (build-cljs-dev)
        (target)
        (speak)))


(deftask follow []
  (comp
   (copy-node-modules)
   (watch)
   (build-dev)
   (target)))
