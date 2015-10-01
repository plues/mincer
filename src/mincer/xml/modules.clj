(ns mincer.xml.modules
  (:gen-class)
  (:require
   [mincer.xml.util :refer [get-xml]]))

(defn extract-semesters [s]
  (map #(Integer/parseInt %) (clojure.string/split s #",")))

(defmulti tree-to-unit-map (comp :tag first))

(defmethod tree-to-unit-map :session [{{:keys [day time duration rhythm]} :attrs}]
  {:type     :session
   :day      day
   :time     (Integer/parseInt time)
   :duration (Integer/parseInt duration)
   :rhythm   (Integer/parseInt rhythm)})

(defmethod tree-to-unit-map :group [{content :content}]
  {:type     :group
   :sessions (map tree-to-unit-map content)})

(defmethod tree-to-unit-map :abstract-unit [{{id :id} :attrs}]
  {:type :abstract-unit-ref
   :id   id})

(defmethod tree-to-unit-map :unit [{{:keys [id title semester]} :attrs content :content}]
  (let [children (map tree-to-unit-map content)
        group-filter (fn [x] (= :group (:type x)))
        {groups true refs false} (group-by group-filter children)]
    {:type     :unit
     :id       id
     :title    title
     :semester (extract-semesters semester)
     :groups   groups
     :refs     refs}))

(defmethod tree-to-unit-map :default [{tag :tag}]
  (throw  (IllegalArgumentException. (name tag))))

(defmulti tree-to-module-map (comp :tag first))

(defmethod tree-to-module-map :abstract-unit [{{:keys [id title type semester]} :attrs}]
  {:id id
   :title title
   :type (keyword type)
   :semester (extract-semesters semester)})

(defmethod tree-to-module-map :module [{{:keys [pordnr title]} :attrs content :content} course]
  (when pordnr 
    {pordnr {:title  title
             :course course
             :pordnr pordnr
             :abstract-units
             (map tree-to-module-map content)}}))

(defmethod tree-to-module-map :modules [{content :content} course]
  (let [modules (map tree-to-module-map content (repeat course))
        filtered-modules (filter identity modules)]
    (when-not (empty? filtered-modules)
      (apply merge filtered-modules))))

(defmethod tree-to-module-map :course [{{:keys [id title]} :attrs content :content}]
                                        ; add course info to modules
                                        ; NOTE: keeping only those courses that have modules (with a pordnr)
  (let [course  {:type  :course
                 :id   id
                 :title title}
        modules (first content)] ; there is only one child in course-tag
    (tree-to-module-map modules course)))

(defmethod tree-to-module-map :course-module-units [{:keys [content]}]
  {:modules (apply merge (map tree-to-module-map content))})

(defmethod tree-to-module-map :units [{:keys [content]}]
  {:units (map tree-to-unit-map content)})

(defmethod tree-to-module-map :default [{:keys [tag]}]
  (throw  (IllegalArgumentException. (name tag))))

(defn transform [{:keys [content]}]
  (apply merge (map tree-to-module-map content)))

(defn process [f]
  (transform (get-xml f)))
