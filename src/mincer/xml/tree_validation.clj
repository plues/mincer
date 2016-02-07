(ns mincer.xml.tree-validation
  (:gen-class)
  (:require
    [mincer.xml.util :refer [freqs]]
    [clojure.tools.logging :as log]))

(defmulti validate :tag)

(defmethod validate :ModulBaum [mb]
  (log/trace (:tag mb))
  (let [pordnrs (flatten (map validate (:content mb)))
        repeated (freqs pordnrs)]
    (doseq [[pordnr count] repeated]
      (log/warn "pordnr" pordnr "used" count "times"))))

(defmethod validate :b [b]
  (log/trace (:tag b))
  (let [pordnrs (flatten (map validate (:content b)))
        ; repeated (freqs pordnrs)
        ; course-name (str (-> b :attrs :name) " - " (-> b :attrs :pversion))
        ]
    ; (doseq [[pordnr count] repeated]
    ;   (log/warn "pordnr" pordnr "used" count "times in module-tree"
    ;             "of course " course-name))
    pordnrs))

(defmethod validate :l [l]
  (log/trace (:tag l))
  (log/trace l)
  (flatten (map validate (:content l))))

(defmethod validate :m [m]
  (log/trace (:tag m))
  (let [pordnr (-> m :attrs :pordnr)]
    (when (nil? pordnr) (log/warn "pordnr missing " (-> m :attrs :name)))
    pordnr))

(defmethod validate :default [tag]
  (log/trace "Ignoring" tag)
  [])

(defn validate-values [xml]
  (validate xml))
