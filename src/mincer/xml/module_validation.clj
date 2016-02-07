(ns mincer.xml.module-validation
  (:gen-class)
  (:require
    [clojure.tools.logging :as log]))

(defn freqs [coll]
  (into {} (filter (fn [[a b]] (> b 1)) (frequencies coll))))

(defmulti validate :tag)

(defmethod validate :units [units]
  (log/trace (:tag units))
  (let [ids (map #(-> % :attrs :id) (:content units))
        repeated (freqs ids)]
    (doseq [[u-id count] repeated]
      (log/warn "Repeated unit id in <units> section:" u-id "appears" count "times"))))

(defmethod validate :modules [modules]
  (log/trace (:tag modules))
  (map #(-> % :attrs :pordnr) (:content modules)))

(defmethod validate :course [course]
  (log/trace (:tag course))
  (let [modules (:content course)]
    (assert (= 1 (count modules)))
    (validate (first modules))))

(defmethod validate :course-module-units [cmu]
  (log/trace (:tag cmu))
  (let [modules (map validate (:content cmu))
        repeated (-> modules flatten freqs) ]
    (doseq [[pordnr count] repeated]
      (log/error "Repeated pordnr in <course-module-units> section:" pordnr "appears" count "times"))))

(defmethod validate :default [tag]
  (log/trace (:tag tag))
  (doseq [c (:content tag)]
    (validate c)))

(defn validate-values [xml]
  (validate xml))
