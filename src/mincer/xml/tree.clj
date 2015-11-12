(ns mincer.xml.tree
  (:gen-class)
  (:require [mincer.xml.util :refer [get-xml]]
            [clojure.tools.logging :as log]
            [clojure.string :refer [trim trim-newline]]))


(defmulti parse-tree :tag)

(defmethod parse-tree :m [{{:keys [name pordnr pflicht]} :attrs}]
    {:type :module
     :name name
     :id pordnr
     :pordnr pordnr
     :mandatory (= pflicht "j")})

(defmethod parse-tree :l [{{:keys [min max name TM ART]} :attrs content :content}]
  (let [children  (remove nil? (mapv parse-tree content))]
    {:type     :level
     :min      (when-not (nil? min) (Integer/parseInt min))
     :max      (when-not (nil? max) (Integer/parseInt max))
     :name     name
     :tm       TM
     :art      ART
     :children children}))

(defmethod parse-tree :b [{{:keys [abschl stg pversion kzfa name]} :attrs content :content}]
  (log/debug {:tag :b :stg stg :pversion pversion :kzfa kzfa :name name :abschl abschl})
  (let [levels  (remove nil? (mapv parse-tree content))]
    {:type     :course
     :degree   abschl
     :course   stg
     :po       pversion
     :kzfa     kzfa ; XXX find out what this means
     :name     name
     :children levels}))

(defmethod parse-tree :ModulBaum [{:keys [content]}]
  (mapv parse-tree content))

                                        ; known but ignored tags
(defmethod parse-tree :regeln [node] (log/debug "Ignoring node regeln"))
(defmethod parse-tree :i [node] (log/debug "Ignoring node i"))

(defn trunc [s n]
    (subs s 0  (min  (count s) n)))

(defn log-cdata [cdata]
    (let [cd (trim (trim-newline cdata))
          msg (trunc cd 50)]
      (log/debug (str "Ignoring CDATA" " '" msg "...'"))))

(defmethod parse-tree :default [arg]
  (let [tag (:tag arg)]
    (when-not (nil? tag)
      (throw  (IllegalArgumentException. (name tag))))
    (log-cdata arg)))

(defn process [f]
  (parse-tree (get-xml f)))
