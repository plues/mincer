(ns mincer.xml.tree
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clojure.string :refer [trim trim-newline upper-case]]
            [mincer.xml.util :refer [get-xml validate]]
            [mincer.xml.tree-validation :refer [validate-values]]))

(def schema (io/resource "mincer/modulbaum.xsd"))

(defmulti parse-tree :tag)

(defmethod parse-tree :m [{{:keys [cp name pordnr pflicht]} :attrs}]
    (log/debug (str "module " [cp name pordnr pflicht]))
    {:type :module
     :name name
     :id pordnr
     :pordnr pordnr
     :cp (when-not (nil? cp) (Integer/parseInt cp))
     :mandatory (= pflicht "j")})

(defmethod parse-tree :l [{{:keys [min min-cp max max-cp name TM ART]} :attrs content :content}]
  (let [children  (remove nil? (pmap parse-tree content))]
    {:type     :level
     :min      (when-not (nil? min) (Integer/parseInt min))
     :max      (when-not (nil? max) (Integer/parseInt max))
     :min-cp   (when-not (nil? min-cp) (Integer/parseInt min-cp))
     :max-cp   (when-not (nil? max-cp) (Integer/parseInt max-cp))
     :name     name
     :tm       TM
     :art      ART
     :children children}))

(defmethod parse-tree :minors [{content :content}]
  (let [children  (remove nil? (pmap parse-tree content))]
    {:type     :minors
     :children children}))

(defmethod parse-tree :minor [{{:keys [stg pversion]} :attrs content :content}]
    {:type :minor
     :short_name  stg
     :po (Integer/parseInt pversion)})

(defmethod parse-tree :b [{{:keys [abschl ignored stg cp pversion kzfa name]} :attrs content :content}]
  (log/debug {:tag :b :stg stg :pversion pversion :kzfa kzfa :name name :abschl abschl})
  (if (= "true" ignored)
    (log/info (str "Course '" name "' marked as ignored"))
    (let [levels  (remove nil? (pmap parse-tree content))]
      {:type     :course
       :degree   abschl
       :course   stg
       :po       (when-not (nil? pversion) (Integer/parseInt pversion))
       :kzfa     (upper-case kzfa) ; XXX find out what this means
       :name     name
       :cp (when-not (nil? cp) (Integer/parseInt cp))
       :children levels})))

(defmethod parse-tree :ModulBaum [{:keys [content attrs]}]
  {:info attrs :levels (filterv #(not (nil? %)) (pmap parse-tree content))})

                                        ; known but ignored tags
(defmethod parse-tree :regeln [node] (log/debug "Ignoring node regeln"))
(defmethod parse-tree :i [node] (log/debug "Ignoring node i"))

(defn trunc [s n]
    (subs s 0  (min  (count s) n)))

(defn log-cdata [cdata]
    (let [cd (trim (trim-newline (str cdata)))
          msg (trunc cd 50)]
      (log/debug (str "Ignoring CDATA" " '" msg "...'"))))

(defmethod parse-tree :default [arg]
  (let [tag (:tag arg)]
    (when-not (nil? tag)
      (throw  (IllegalArgumentException. (name tag))))
    (log-cdata arg)))

(defn process [f]
  (validate schema f)
  (log/info "validation passed")
  (let [xml (get-xml f)]
    (validate-values xml)
    (log/info "value validation done")
    (parse-tree (get-xml f))))
