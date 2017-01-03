(ns shboard.util
  (:require [shboard.state :as state]))

(defn assoc-opts [opt opts] (apply hash-map (cons opt opts)))

(defn debug-log [x & xs] (if (:debug @state/db) (apply println (cons x xs))))