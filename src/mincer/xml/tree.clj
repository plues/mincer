(ns mincer.xml.tree
  (:gen-class)
  (:require [mincer.xml.util :refer [get-xml]]
            [clojure.tools.logging :as log]
            [clojure.string :refer [trim trim-newline]]))


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
  (let [children  (remove nil? (mapv parse-tree content))]
    {:type     :level
     :min      (when-not (nil? min) (Integer/parseInt min))
     :max      (when-not (nil? max) (Integer/parseInt max))
     :min-cp   (when-not (nil? min-cp) (Integer/parseInt min-cp))
     :max-cp   (when-not (nil? max-cp) (Integer/parseInt max-cp))
     :name     name
     :tm       TM
     :art      ART
     :children children}))

(defmethod parse-tree :b [{{:keys [abschl stg cp pversion kzfa name]} :attrs content :content}]
  (log/debug {:tag :b :stg stg :pversion pversion :kzfa kzfa :name name :abschl abschl})
  (let [levels  (remove nil? (mapv parse-tree content))]
    {:type     :course
     :degree   abschl
     :course   stg
     :po       (when-not (nil? pversion) (Integer/parseInt pversion))
     :kzfa     kzfa ; XXX find out what this means
     :name     name
     :cp (when-not (nil? cp) (Integer/parseInt cp))
     :children levels}))

(defmethod parse-tree :ModulBaum [{:keys [content]}]
  (mapv parse-tree content))

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
  (parse-tree (get-xml f)))
