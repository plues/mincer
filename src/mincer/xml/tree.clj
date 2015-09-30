(ns mincer.xml.tree
  (:gen-class)
   (:require [mincer.xml.util :refer [get-xml]]))


(defn build-key [attrs]
  (clojure.string/join "" (map attrs [:stg :kzfa :pversion])))

(defmulti parse-tree (fn [x & args] (:tag x)))

(defmethod parse-tree :m [node]
  (let [attrs (:attrs node)]
    {:type :module
     :name (:name attrs)
     :id (:pordnr attrs)
     :pordnr (:pordnr attrs)
     :mandatory (= (:pflicht attrs) "j")}))

(defmethod parse-tree :l [node]
  (let [children  (mapv parse-tree (:content node))
        attrs (:attrs node)]
    {:type     :level
     :min      (:min attrs)
     :max      (:max attrs)
     :name     (:name attrs)
     :tm       (:TM attrs)
     :art      (:ART attrs)
     :children children}))

(defmethod parse-tree :b [node]
  (let [levels  (mapv parse-tree (:content node))
        attrs (:attrs node)]
    {(build-key attrs) {:type     :course
                        :degree   (:abschl attrs)
                        :course   (:stg attrs)
                        :po       (:pversion attrs)
                        :kzfa     (:kzfa attrs) ; XXX find out what this means
                        :name     (:name attrs)
                        :children levels}}))

(defmethod parse-tree :ModulBaum [node]
  (apply merge (map parse-tree (:content node))))


(defmethod parse-tree :default [node]
  (throw  (IllegalArgumentException. (name  (:tag node)))))

(defn process [f]
  (parse-tree (get-xml f)))
