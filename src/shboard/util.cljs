(ns shboard.util
 (:require [clojure.set :as set]
           [cljs.spec :as spec]))

(defn same-values?
 "Compares the value at given key in col1 and col2. Returns true if they are the same,
  including if they are both missing. Otherwise false."
 [key col1 col2]
 (= (get col1 key) (get col2 key)))

(defn same-values-in?
 "Like same-values?, but uses a get-in path instead of a single key."
 [path col1 col2]
 (= (get-in col1 path) (get-in col2 path)))

(defn strict-superset?
 [s1 s2]
 (and (not= s1 s2) (set/superset? s1 s2)))

(defn strict-subset?
 [s1 s2]
 (and (not= s1 s2) (set/subset? s1 s2)))
 
(def keyset
 "Returns the set of keys for given map."
 (comp set keys))

(defn explain-valid?
 "Similar to clojure.spec/valid? except it prints the explained problems if validation fails."
 [spec x]
 (if-let [explained-errors (spec/explain-data spec x)]
  (spec/explain-out explained-errors)
  true))
 
(defn clamp
 "Clamps n between low and high (inclusive)"
 [n low high]
 (-> n (max low) (min high)))

(defn print-and-return
 [x prefix]
 (println prefix x)
 x)

(defn merge-deep
  "Merges maps together. If values are maps, they will be merged recursively."
  [& maps]
  (apply merge-with #(if (and (map? %1) (map? %2)) (merge-deep %1 %2) %2) maps))

(defn build-assoc-opts
 "Accepts a hash-map, with optional default options,
  and returns a flat seq of associative otpions for 'apply'.
  e.g. (build-assoc-opts {:a 1 :b 2} :b 3 :c 4) -> [:a 1 :b 2 :c 4]"
 [initial-opts & {:as default-opts}]
 (->> initial-opts
  (merge-deep default-opts)
  (apply concat)
  (into [])))

(defn in-range?
 "Returns true if num is in the range [low..high)"
 [num low high]
 (and (<= low num) (> high num)))

(defn without-keys
 "Returns a new map with given keys removed."
 [keys map]
 (let [keys (into #{} keys)]
  (into {} (filter (comp not keys first)
                   map))))

(defn kw->str
 "Accepts a :keyword and returns a stringified version without the leading colon."
 [kw]
 (->> kw (str) (drop 1) (apply str)))