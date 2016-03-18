(ns mincer.xml.util
  (:gen-class)
  (:require
    [clojure.xml :as xml]
    [clojure.string :as string])
  (:import
    (javax.xml.transform.stream StreamSource)
    (javax.xml.validation SchemaFactory)
    (javax.xml XMLConstants)))

(defn get-xml [filename]
  (xml/parse
    (java.io.ByteArrayInputStream.
      (.getBytes
        (slurp filename)))))

(defn validate [schema-file xml-file]
  (let [sf (SchemaFactory/newInstance XMLConstants/W3C_XML_SCHEMA_NS_URI)
        schema (.newSchema sf schema-file)
        validator (.newValidator schema)]
    (.validate validator (StreamSource. xml-file))))

(defn freqs [coll]
  (into {} (filter (fn [[a b]] (> b 1)) (frequencies coll))))

(defn ranges [coll]
  (println (type (first coll)))
  (filter (fn [sem] (some #(or (< % 1) (> % 6))
      (map #(Integer/parseInt %) (string/split sem #",")))) coll))
