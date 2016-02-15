(ns mincer.xml.module-validation
  (:gen-class)
  (:require
    [mincer.xml.util :refer [freqs]]
    [clojure.tools.logging :as log]))

(defmulti validate :tag)

(defmethod validate :units [units]
  (log/trace (:tag units))
  (let [ids (map #(-> % :attrs :id) (:content units))
        repeated (freqs ids)]
    (doseq [[u-id count] repeated]
      (log/error "Repeated unit id in <units> section:" u-id "appears" count "times"))))

(defmethod validate :modules [node]
  (log/trace (:tag node))
  (let [pordnrs  (map #(-> % :attrs :pordnr) (:content node))
        repeated (freqs pordnrs)]
    (doseq [[pordnr count] repeated]
      (log/error "Repeated pordnr in <modules> section:" pordnr "appears" count "times"))))

(defmethod validate :default [tag]
  (log/trace (:tag tag))
  (doseq [c (:content tag)]
    (validate c)))

(defn validate-values [xml]
  (validate xml))
