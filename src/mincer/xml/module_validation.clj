(ns mincer.xml.module-validation
  (:gen-class)
  (:require
    [clojure.set :refer [difference]]
    [clojure.tools.logging :as log]
    [clojure.walk :refer [prewalk]]
    [mincer.xml.util :refer [freqs ranges]]))

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



(defn collect-abstract-units [n]
  (let [walk-fn (fn  [node]
                  (if (= :abstract-unit (:tag node))
                    ; we found a abstract-unit node, fetch id
                    (-> node :attrs :id)
                    ; continue traversing the tree
                    (:content node)))
        content (prewalk walk-fn n)]
    (set (filter #(-> % nil? not) (flatten content)))))

(defmulti validate-abstract-units :tag)

(defmethod validate-abstract-units :units [node]
  {:units (collect-abstract-units node)})

(defmethod validate-abstract-units :modules [node]
  {:modules (collect-abstract-units node)})

(defmethod validate-abstract-units :data [node]
  (let [{:keys [units modules]} (apply merge (map
                                               validate-abstract-units
                                               (:content node)))]
    (log/trace "UNITS" units)
    (log/trace "MODULES" modules)
    (doseq [au (difference modules units)]
      (log/warn "Abstract-Unit with ID:" au "has no units associated to it and will be ignored."))))


(defmulti validate-modules :tag)

(defmethod validate-modules :units [node])
(defmethod validate-modules :default [node]
  (doseq [n (:content node)]
    (validate-modules n)))

(defmethod validate-modules :module [node]
  (let [n-elective (count
                     (filter #(= "e" (-> % :attrs :type)) (:content node)))
        module (:attrs node)
        required-elective (Integer/parseInt (:elective-units module))]
    (log/trace "MODULE" node)
    (when (< n-elective required-elective)
      (log/error "Module with ID:" (:id module)
                 "and PORDNR:" (:pordnr module)
                 "has fewer elective abstract-units" n-elective
                 "than the required" (:elective-units module)))))

(defn validate-values [xml]
  (binding [errors false]
    (validate xml)
    (validate-modules xml)
    (validate-abstract-units xml)
    (if errors
      (throw (IllegalArgumentException. "Module data contains validation errors")))))
