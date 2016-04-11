(ns mincer.xml.module-validation
  (:gen-class)
  (:require
    [mincer.xml.util :refer [freqs ranges]]
    ; [mincer.xml ValidationError]
    [clojure.tools.logging :as log]))

(def ^:dynamic errors)

(defmulti validate :tag)

(defmethod validate :units [units]
  (log/trace (:tag units))
  (let [ids (map #(-> % :attrs :id) (:content units))
        repeated (freqs ids)]
    (if-not (empty? repeated)
      (set! errors true))
    (doseq [[u-id count] repeated]
      (log/error "Repeated unit id in <units> section:" u-id "appears" count "times")))
  (let [semesters (map #(-> % :attrs :semester) (:content units))
        ranges (ranges semesters 6)]
      (if-not (empty? ranges)
        (set! errors true))
      (doseq [semester ranges]
        (log/error "Semester out of range in <units> section:" semester))))

(defmethod validate :modules [node]
  (log/trace (:tag node))
  (let [pordnrs  (map #(-> % :attrs :pordnr) (:content node))
        repeated (freqs pordnrs)]
    (if-not (empty? repeated)
      (set! errors true))
    (doseq [[pordnr count] repeated]
      (log/error "Repeated pordnr in <modules> section:" pordnr "appears" count "times"))))

(defmethod validate :default [tag]
  (log/trace (:tag tag))
  (doseq [c (:content tag)]
    (validate c)))

(defn validate-values [xml]
  (binding [errors false]
    (validate xml)
    (if errors
      (throw (IllegalArgumentException. "Module data contains validation errors")))))
