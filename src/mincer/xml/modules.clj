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
  (throw  (IllegalArgumentException. (:tag node))))

(defmulti tree-to-module-map (fn [x & args] (:tag x)))

(defmethod tree-to-module-map :abstract-unit [node]
  (println "Tag " (:tag node))
  (let [attrs  (:attrs node)]
    {:id (:id attrs)
     :title (:title attrs)
     :type (keyword (:type attrs))
     :semester (extract-semesters (:semester attrs))}))

(defmethod tree-to-module-map :module [node]
  (println "Tag " (:tag node))
  (let [attrs  (:attrs node)
        pordnr (:pordnr attrs)]
    ; use pordnr as key once it is available
    (when (not (nil? pordnr))
      {pordnr {:title (:title attrs)
                        :abstract-units
                          (map tree-to-module-map (:content node))}})))

(defmethod tree-to-module-map :modules [node]
  (println "Tag " (:tag node))
  (let [modules (map tree-to-module-map (:content node))
        filtered-modules (remove nil? modules)]
    (println "modules" modules)
    (println "filtered" filtered-modules)
    (if (not= 0 (count filtered-modules))
      (apply merge filtered-modules)
      [])))

(defmethod tree-to-module-map :course [node]
  (println "Tag " (:tag node))
  (println "Content" (:content node))
  ; add course info to modules
  (let [modules (tree-to-module-map
                  (first (:content node))) ; there is only one child in course-tag
        attrs (:attrs node)]
    {:type :course
     :id (:id attrs)
     :title (:title attrs)
     :modules modules}))

(defmethod tree-to-module-map :course-module-units [node]
  (println "Tag " (:tag node))
  {:cmu (map tree-to-module-map (:content node))})

(defmethod tree-to-module-map :units [node] 
  (println "Tag " (:tag node))
  {:units (map tree-to-unit-map (:content node))})

(defmethod tree-to-module-map :default [node]
  (println "Tag " (:tag node))
  (throw  (IllegalArgumentException. (:tag node)))
)

(defn transform [node]
  (apply merge (map tree-to-module-map (:content node))))

(defn process [f]
  (transform (get-xml f)))
