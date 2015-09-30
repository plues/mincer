(ns mincer.xml.util
  (:gen-class)
  (:require
    [clojure.xml :as xml]))

(defn get-xml [filename]
  (xml/parse
    (java.io.ByteArrayInputStream.
      (.getBytes
        (slurp filename)))))
