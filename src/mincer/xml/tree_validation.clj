(ns mincer.xml.tree-validation
  (:gen-class)
  (:require
    [mincer.xml.util :refer [freqs genKey]]
    ; [mincer.xml ValidationError]
    [clojure.set :refer [subset?]]
    [clojure.tools.logging :as log]))

(defmulti validate :tag)

(def ^:dynamic errors)

(defmethod validate :ModulBaum [mb]
  (log/trace (:tag mb))
  ; validate pordnr
  (dorun (flatten (map validate (:content mb))))
  ; validate course names
  (let [course-names  (map #(-> % :attrs :name) (:content mb))
        f (freqs course-names)]
    (if-not (empty? f)
      (set! errors true))
    (doseq [[name count] f]
      (log/error "Repeated course name in <ModulBaum> section:" name "appears"
                 count "times")))
  ; validate keys
  (let [quadruple (map #(vector
                          (-> % :attrs :abschl)
                          (-> % :attrs :stg)
                          (-> % :attrs :kzfa)
                          (-> % :attrs :pversion)) (:content mb))
        keys (map genKey quadruple)
        repeated (freqs keys)]
    (if-not (empty? repeated)
      (set! errors true))
    (doseq [[key count] repeated]
      (log/error "Repeated key in <ModuleData> section:" key "appears" count "times"))))

(defmethod validate :b [b]
  (log/trace (:tag b))
  (flatten (map validate (:content b))))

(defmethod validate :l [l]
  (log/trace (:tag l))
  (log/trace l)
  ; validate mixed l and m tags in levels
  ; checking if content containts l and m tags 
  (if (subset? #{:l :m} (set (map :tag (:content l))))
    (do
      (set! errors true)
      (log/error "level containts l and m tags as children in level " (-> l :attrs :name))))
  (flatten (map validate (:content l))))

(defmethod validate :minors [minors]
  (log/trace (:tag minors))
  ; only minor tags
  (if (not (= #{:minor} (set (map :tag (:content minors)))))
    (log/error "Tag 'minors' can only contain minor-Tags."))
  (flatten (map validate (:content minors))))

(defmethod validate :minor [minor]
  (log/trace (:tag minor))
  (let [stg (-> minor :attrs :stg)
        kzfa (-> minor :attrs :kzfa)]
    (do (when (nil? stg) (log/error "Missing 'stg' in minor tag."))
        (when (nil? kzfa) (log/error "Missing 'kzfa' in minor tag."))
        (when (not (= kzfa "N")) (log/error "Attribute 'kzfa' in minor tag has to be 'N'.")))))

(defmethod validate :m [m]
  (log/trace (:tag m))
  (let [pordnr (-> m :attrs :pordnr)]
    (when (nil? pordnr) (log/error "pordnr missing " (-> m :attrs :name)))
    pordnr))

(defmethod validate :default [tag]
  (log/trace "Ignoring" tag)
  [])

(defn validate-modules [xml]
  (let [modules (group-by (fn [node] (-> node :attrs :pordnr)) 
                          (filter (fn [node] (= :m (:tag node)))
                                  (tree-seq (fn [node] (not (= :m (:tag node))))
                                  (fn [node] (:content node))
                                  xml)))]
      (doseq [[pordnr values] modules]
        (when (not (apply = (map #(:name %) (map #(:attrs %) values))))
          (set! errors true)
          (log/error "Attributes of modules with pordnr" pordnr "differ (maybe a copy-paste error?)")))))
(defn validate-values [xml]
  (binding [errors false]
    (validate xml)
    (validate-modules xml)
    (if errors
      (throw (IllegalArgumentException. "Module tree contains validation errors.")))))