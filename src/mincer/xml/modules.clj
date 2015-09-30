(ns mincer.xml.modules
  (:gen-class)
  (:require
    [mincer.xml.util :refer [get-xml]]))

(defn extract-semesters [s]
  (map #(Integer/parseInt %) (clojure.string/split s #",")))

(defmulti tree-to-unit-map (fn [x & args] (:tag x)))

(defmethod tree-to-unit-map :session [node]
  (let [attrs (:attrs node)]
    {:type     :session
     :day      (:day attrs)
     :time     (Integer/parseInt (:time attrs))
     :duration (Integer/parseInt (:duration attrs))
     :rhythm   (Integer/parseInt(:rhythm attrs))}))

(defmethod tree-to-unit-map :group [node]
  {:type     :group
   :sessions (map tree-to-unit-map (:content node))})

(defmethod tree-to-unit-map :abstract-unit [node]
  {:type :abstract-unit-ref
   :id   (:id (:attrs node))})

(defmethod tree-to-unit-map :unit [node]
  (let [attrs (:attrs node)
        children (map tree-to-unit-map (:content node))
        group-filter (fn [x] (= :group (:type x)))
        groups (filter group-filter children)
        refs (filter (complement group-filter) children)]
    {:type     :unit
     :id       (:id attrs)
     :title    (:title attrs)
     :semester (extract-semesters (:semester attrs))
     :groups   groups
     :refs     refs}))

(defmethod tree-to-unit-map :default [node]
  (throw  (IllegalArgumentException. (name  (:tag node))))) 

(defmulti tree-to-module-map (fn [x & args] (:tag x)))

(defmethod tree-to-module-map :abstract-unit [node]
  (let [attrs  (:attrs node)]
    {:id (:id attrs)
     :title (:title attrs)
     :type (keyword (:type attrs))
     :semester (extract-semesters (:semester attrs))}))

(defmethod tree-to-module-map :module [node course]
  (let [attrs  (:attrs node)
        pordnr (:pordnr attrs)]
    ; use pordnr as key once it is available
    (when-not (nil? pordnr)
      {pordnr {:title  (:title attrs)
               :course course
               :pordnr pordnr
                       :abstract-units
                          (map tree-to-module-map (:content node))}})))

(defmethod tree-to-module-map :modules [node course]
  (let [modules (map tree-to-module-map (:content node) (repeat course))
        filtered-modules (remove nil? modules)]
    (when-not (= 0 (count filtered-modules))
      (apply merge filtered-modules))))

(defmethod tree-to-module-map :course [node]
  ; add course info to modules
  ; NOTE: keeping only those courses that have modules (with a pordnr)
  (let [attrs   (:attrs node)
        course  {:type  :course
                 :id    (:id attrs)
                 :title (:title attrs)}
        modules (first (:content node))] ; there is only one child in course-tag
    (tree-to-module-map modules course)))

(defmethod tree-to-module-map :course-module-units [node]
  {:modules (apply merge (map tree-to-module-map (:content node)))})

(defmethod tree-to-module-map :units [node]
  {:units (map tree-to-unit-map (:content node))})

(defmethod tree-to-module-map :default [node]
  (throw  (IllegalArgumentException. (name  (:tag node)))))

(defn transform [node]
  (apply merge (map tree-to-module-map (:content node))))

(defn process [f]
  (transform (get-xml f)))
