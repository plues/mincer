(ns mincer.xml.util
  (:gen-class)
  (:require
    [clojure.set :refer [difference]]
    [clojure.xml :as xml]))

(defn get-xml [filename]
  (xml/parse
    (java.io.ByteArrayInputStream.
      (.getBytes
        (slurp filename)))))


(defn- no-content [tag] (into {} (filter (fn [[k v]] (not (= :content k))) tag)))
(defn- attr-names [tag] (-> tag :attrs keys set))


(defn check-tag [exp tag]
  (if-not (nil? (:tag tag))
    (assert (= exp (:tag tag)) (str "Expected " exp " tag, but was " (no-content tag)))
    (throw (AssertionError. (str "Expected " exp " tag, but was non-tag " tag)))))


(defn check-attributes [name tag attr-set]
  (let [attrs (attr-names tag)
        missing (difference attr-set attrs)]
    (assert (= 0 (count missing))
            (str name " tag " tag " missing required attributes " missing))))


(defn check-node
  ([node-name attr-set next-fn] (check-node node-name node-name attr-set next-fn))
  ([node-name attr-name attr-set next-fn] (fn [elem]
                                   (check-tag node-name elem)
                                   (check-attributes attr-name elem attr-set)
                                   (when-not (nil? next-fn)
                                     (doall (map next-fn (:content elem)))))))


