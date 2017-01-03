(set-env!
 :source-paths    #{"src"}
 :dependencies '[[adzerk/boot-cljs          "1.7.228-2"  :scope "test"]
                 [org.clojure/clojurescript "1.7.228"]
                 [org.clojure/tools.cli "0.3.5"]])

(require
 '[adzerk.boot-cljs      :refer [cljs]])

(deftask build []
  (comp (speak)
        (cljs :source-map true
         :optimizations :none
         :compiler-options {:target :nodejs})
        (target :no-clean true)))


(deftask follow []
  (comp
   (watch)
   (build)))
