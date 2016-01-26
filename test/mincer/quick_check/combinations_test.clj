(ns mincer.quick_check.combinations_test
  (:require
    [clojure.test :refer :all]
    [clojure.test.check :as tc]
    [clojure.test.check.generators :as gen]
    [clojure.test.check.properties :as prop]
    [mincer.module-combinations :refer :all]))

(def test_tree
  [{
    :required [3]
    :children
      [{
        :required [1 2 3]
        :children [{:id 1 :children []} {:id 2 :children []} {:id 3 :children []}]}
      {
        :required [1 2]
        :children [{:id 4 :children []} {:id 5 :children []} {:id 6 :children []}]}]}])

(defn length_check "Check if count of sel is element of req"[sel req]
  (some #(= (count sel) %) req))

(declare check_children)

(defn check_tree_node "Check if there are children for a single node" [tree_node]
  (if (empty? (tree_node :children))
    (println (tree_node :id))
    (check_children (tree_node :children))))

(defn check_children [nodes]
  (for [n nodes]
    (check_tree_node n)))

(defn ascending? [coll]
  (every? (fn [[a b ]] (<= a b))
    (partition 2 1 coll)))

(def property
  (prop/for-all [v (gen/vector gen/int)]
    (let [s (sort v)]
      (and (= (count v) (count s))
        (ascending? s)))))

(deftest quick-check
  (do
    newline
    (println (tc/quick-check 100 property))))