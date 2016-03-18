(ns mincer.xml.tree-validation
  (:gen-class)
  (:require
    [mincer.xml.util :refer [freqs genKey]]
    [clojure.tools.logging :as log]))

(defmulti validate :tag)

(defmethod validate :ModulBaum [mb]
  (log/trace (:tag mb))
  ; validate pordnr
  (doall (flatten (map validate (:content mb))))
  ; validate course names
  (let [course-names  (map #(-> % :attrs :name) (:content mb))]
    (doseq [[name count] (freqs course-names)]
      (log/error "Repeated course name in <ModuleData> section:" name "appears"
                 count "times")))
  ; validate keys
  (let [quadruple (map #(vector
                          (-> % :attrs :abschl)
                          (-> % :attrs :stg)
                          (-> % :attrs :kzfa)
                          (-> % :attrs :pversion)) (:content mb))
        keys (map genKey quadruple)
        repeated (freqs keys)]
    (doseq [[key count] repeated]
      (log/error "Repeated key in <ModuleData> section: " key "appears" count "times"))))

(defmethod validate :b [b]
  (log/trace (:tag b))
  (flatten (map validate (:content b))))

(defmethod validate :l [l]
  (log/trace (:tag l))
  (log/trace l)
  (flatten (map validate (:content l))))

(defmethod validate :m [m]
  (log/trace (:tag m))
  (let [pordnr (-> m :attrs :pordnr)]
    (when (nil? pordnr) (log/warn "pordnr missing " (-> m :attrs :name)))
    pordnr))

(defmethod validate :default [tag]
  (log/trace "Ignoring" tag)
  [])

(defn validate-values [xml]
  (validate xml))
