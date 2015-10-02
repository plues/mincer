(ns mincer.xml.tree
  (:gen-class)
  (:require [mincer.xml.util :refer [get-xml]]))


(defmulti parse-tree :tag)

(defmethod parse-tree :m [{{:keys [name pordnr pflicht]} :attrs}]
    {:type :module
     :name name
     :id pordnr
     :pordnr pordnr
     :mandatory (= pflicht "j")})

(defmethod parse-tree :l [{{:keys [min max name TM ART]} :attrs content :content}]
  (let [children  (mapv parse-tree content)]
    {:type     :level
     :min      (when-not (nil? min) (Integer/parseInt min))
     :max      (when-not (nil? max) (Integer/parseInt max))
     :name     name
     :tm       TM
     :art      ART
     :children children}))

(defmethod parse-tree :b [{{:keys [abschl stg pversion kzfa name]} :attrs content :content}]
  (let [levels  (remove nil? (mapv parse-tree content))]
    {(str stg kzfa pversion) {:type     :course
                              :degree   abschl
                              :course   stg
                              :po       pversion
                              :kzfa     kzfa ; XXX find out what this means
                              :name     name
                              :children levels}}))

(defmethod parse-tree :ModulBaum [{:keys [content]}]
  (apply merge (map parse-tree content)))

                                        ; known but ignored tags
(defmethod parse-tree :regeln [node] (println "Ignoring node regeln"))
(defmethod parse-tree :i [node] (println "Ignoring node i"))

(defmethod parse-tree :default [{:keys [tag]}]
  (throw  (IllegalArgumentException. (name tag))))

(defn process [f]
  (parse-tree (get-xml f)))
