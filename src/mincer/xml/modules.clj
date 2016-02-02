(ns mincer.xml.modules
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clojure.string :refer [upper-case]]
            [clojure.tools.logging :as log]
            [mincer.xml.util :refer [get-xml validate]]))

  (def schema (io/resource "mincer/moduldaten.xsd"))

(defn extract-semesters [s]
  (map #(Integer/parseInt %) (clojure.string/split s #",")))

(defn map-half-semester [x]
  (case x
    "1" 1
    "first" 1
    "2" 2
    "second" 2
    0))

(defmulti tree-to-unit-map :tag)

(defmethod tree-to-unit-map :session [{{:keys [day time duration rhythm]} :attrs}]
  {:type     :session
   :day      day
   :time     (Integer/parseInt time)
   :duration (Integer/parseInt duration)
   :rhythm   (Integer/parseInt rhythm)})

(defmethod tree-to-unit-map :group [{{:keys [half-semester]} :attrs content :content}]
  {:type          :group
   :half-semester (map-half-semester half-semester)
   :sessions      (map tree-to-unit-map content)})

(defmethod tree-to-unit-map :abstract-unit [{{id :id} :attrs}]
  {:type :abstract-unit-ref
   :id   (upper-case id)})

(defmethod tree-to-unit-map :unit [{{:keys [id title semester]} :attrs content :content}]
  (let [children (map tree-to-unit-map content)
        group-filter (fn [x] (= :group (:type x)))
        {:keys [group abstract-unit-ref]} (group-by #(:type %) children)]
    {:type          :unit
     :id            (upper-case id)
     :title         title
     :semester      (extract-semesters semester)
     :groups        group
     :refs          (or abstract-unit-ref [])}))

(defmethod tree-to-unit-map :default [arg]
  (let [tag (:tag arg)]
    (log/debug "Invalid key" tag)
    (throw  (IllegalArgumentException. (if-not (nil? tag)
                                         (name tag)
                                         (arg))))))

(defmulti tree-to-module-map :tag)

(defmethod tree-to-module-map :abstract-unit [{{:keys [id title type semester]} :attrs}]
  (log/debug (str id " " title " " type " " semester))
  {:id (upper-case id)
   :title title
   :type (keyword type)
   :semester (extract-semesters semester)})

(defmethod tree-to-module-map :module [{{:keys [id pordnr title elective-units]} :attrs content :content} course]
  (log/debug (str "MODULE id:" id " title: '" title "' pordnr " pordnr))
  (when pordnr
    {pordnr {:title  title
             :key (upper-case id)
             :course course
             :pordnr pordnr
             :elective-units (Integer/parseInt (or elective-units "0"))
             :abstract-units (map tree-to-module-map content)}}))

(defmethod tree-to-module-map :modules [{content :content} course]
  (let [modules (map tree-to-module-map content (repeat course))
        filtered-modules (filter identity modules)]
    (when-not (empty? filtered-modules)
      (apply merge filtered-modules))))

(defmethod tree-to-module-map :course [{{:keys [id title]} :attrs content :content}]
                                        ; add course info to modules
                                        ; NOTE: keeping only those courses that have modules (with a pordnr)
  (let [course  {:type  :course
                 :key   (upper-case id)
                 :title title}
        modules (first content)] ; there is only one child in course-tag
    (tree-to-module-map modules course)))

(defmethod tree-to-module-map :course-module-units [{:keys [content]}]
  {:modules (apply merge (map tree-to-module-map content))})

(defmethod tree-to-module-map :units [{:keys [content]}]
  {:units (map tree-to-unit-map content)})

(defmethod tree-to-module-map :default [arg]
  (let [tag (:tag arg)]
    (log/debug "Invalid key" tag)
      (throw  (IllegalArgumentException. (str "ARG: "(if-not (nil? tag) (name tag) (arg)))))))

(defn transform [{:keys [content]}]
  (apply merge (map tree-to-module-map content)))

(defn process [f]
  (validate schema f)
  (log/info "validation passed")
  (transform (get-xml f)))
