(ns mincer.xml.tree-test-cp
  (:require [clojure.test :refer :all]
            [mincer.xml.tree-cp-test-data :refer :all]
            [mincer.xml.tree-validation :refer :all]
            [mincer.xml.tree :refer :all]))

(def result-cp-module {:type :module
                    :name "MV01: Makroökonomik"
                    :id "40553"
                    :cp 6
                    :pordnr "40553"
                    :mandatory false})

(def result-cp-module-2 {:type :module
                    :name "MV03: Mikroökonomik"
                    :id "40554"
                    :pordnr "40554"
                    :cp 7
                    :mandatory true})

(def result-cp-level {:art nil
           :children [{:art nil
                       :children [result-cp-module]
                       :max nil
                       :min nil
                       :min-cp 10
                       :max-cp 20
                       :name "Area 1"
                       :tm nil
                       :type :level}
                      {:art nil
                       :children [result-cp-module-2 result-cp-module]
                       :max nil
                       :min nil
                       :min-cp 5
                       :max-cp 30
                       :name "Area 2"
                       :tm nil
                       :type :level}
                      {:art nil
                       :children [result-cp-module]
                       :max nil
                       :min nil
                       :min-cp 10
                       :max-cp 100
                       :name "Area 3"
                       :tm nil
                       :type :level}]
           :max nil
           :min nil
           :min-cp 40
           :max-cp 60
           :name "Wahlpflichtmodule"
           :tm nil
           :type :level})

(def result-course {:type :course
                    :degree "bk"
                    :kzfa "H"
                    :cp 91
                    :name "Kernfach Philosophie"
                    :po 2011
                    :course "phi"
                    :children [result-cp-level]})

(deftest test-parse-m-tag
  (is (= result-cp-module (parse-tree m-cp-tag)))
  (is (= result-cp-module-2 (parse-tree m-cp-tag-2)))
  )


(deftest test-parse-l-tag
  (is (= result-cp-level (parse-tree nested-l-cp-tag))))

(deftest test-parse-b-tag
  (is (= result-course
         (parse-tree b-cp-tag))))

(deftest test-parse-modulbaum
  (is (= {:info nil :levels [result-course]}
         (parse-tree modulbaum-cp-tag))))

(deftest test-ignored-tags-in-b
  (let [course (parse-tree b-cp-tag-with-regeln)
        children (:children course)]
    (is (= 1 (count children)))
    (is (not-any? nil? children))))

(deftest test-validate-cp-based-level-with-missing-cp-data
  (binding [errors false]
    (validate-cp l-cp-tag-missing-data)
    (is errors)))
