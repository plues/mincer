(ns mincer.bitvector
  (:gen-class)
  (:import
    (java.util BitSet)))

(defn bitvector
  ([] (BitSet.))
  ([n] (BitSet. n)))

(defn set-bit!
  ([bv b]
   (.set bv b)
   bv)
  ([bv b value]
   (.set bv b value)
   bv))

(defn get-bit [bv b] (.get bv b))

(defn bytes [bv] (.toByteArray bv))
