(ns mincer.xml.tree-test
  (:require [clojure.test :refer :all]
            [mincer.xml.tree-test-data :refer :all]
            [mincer.xml.tree :refer :all]))

(def result-module {:type :module
                    :name "Logik I"
                    :id "29381"
                    :cp nil
                    :pordnr "29381"
                    :mandatory true})

(def result-module-2 {:type :module
                    :name "Grundlagen"
                    :id "29380"
                    :cp nil
                    :pordnr "29380"
                    :mandatory false})

(def result-level {:type :level :min 4 :max 6 :name  "Basiswahlpflichtmodule"
                   :tm nil
                   :art nil
                   :children [
                              {:type :level
                               :min 1
                               :max 2
                               :name  "Theoretische Philosophie"
                               :tm  nil
                               :art nil
                               :children [result-module]}
                              {:type :level
                               :min 1
                               :max 2
                               :name "Praktische Philosophie"
                               :tm "TM"
                               :art nil
                               :children [result-module result-module-2]
                               }
                              {:type :level
                               :min 2
                               :max 4
                               :name "Geschichte der Philosophie"
                               :tm nil
                               :art "ART"
                               :children [result-module]}
                              ]})

(def result-course {:type :course
                    :degree "bk"
                    :kzfa "H"
                    :name "Kernfach Philosophie"
                    :po "2011"
                    :course "phi"
                    :children [result-level]})

(deftest test-parse-m-tag
  (is (= result-module (parse-tree m-tag)))
  (is (= result-module-2 (parse-tree m-tag-2))))


(deftest test-parse-l-tag (is (= (parse-tree nested-l-tag) result-level)))

(deftest test-parse-b-tag
  (is (= result-course
         (parse-tree b-tag))))

(deftest test-parse-modulbaum
  (is (= [result-course]
         (parse-tree modulbaum-tag))))

(deftest test-ignored-tags-in-b
  (let [course (parse-tree b-tag-with-regeln)
        children (:children course)]
    (is (= 1 (count children)))
    (is (not-any? nil? children))))
