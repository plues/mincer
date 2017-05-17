(ns mincer.xml.tree-test
  (:require [clojure.test :refer :all]
            [mincer.xml.tree-test-data :refer :all]
            [mincer.xml.tree :refer :all]
            [mincer.xml.tree-validation :refer :all]))

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
                   :min-cp nil
                   :max-cp nil
                   :tm nil
                   :art nil
                   :children [
                              {:type :level
                               :min 1
                               :max 2
                               :min-cp nil
                               :max-cp nil
                               :name  "Theoretische Philosophie"
                               :tm  nil
                               :art nil
                               :children [result-module]}
                              {:type :level
                               :min 1
                               :max 2
                               :min-cp nil
                               :max-cp nil
                               :name "Praktische Philosophie"
                               :tm "TM"
                               :art nil
                               :children [result-module result-module-2]
                               }
                              {:type :level
                               :min 2
                               :max 4
                               :min-cp nil
                               :max-cp nil
                               :name "Geschichte der Philosophie"
                               :tm nil
                               :art "ART"
                               :children [result-module]}
                              ]})

(def result-course  {:type :course
                    :degree "bk"
                    :kzfa "H"
                    :name "Kernfach Philosophie"
                    :po 2011
                    :cp nil
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
  (is (= {:info nil :levels [result-course]}
         (parse-tree modulbaum-tag))))

(deftest test-ignored-tags-in-b
  (let [course (parse-tree b-tag-with-regeln)
        children (:children course)]
    (is (= 1 (count children)))
    (is (not-any? nil? children))))

; tree validation
(deftest test-validate-b-tag-with-minors
  (validate b-tag-with-minors)
  (validate {:tag :minors, :attrs nil, :content [{:tag :minor, :attrs {:kzfa "N", :stg "phy", :po 2013}, :content nil} {:tag :minor, :attrs {:kzfa "N", :stg "bio", :po 2013}, :content nil} {:tag :minor, :attrs {:kzfa "N", :stg "che", :po 2013}, :content nil} {:tag :minor, :attrs {:kzfa "N", :stg "mat", :po 2013} :content nil} {:tag :minor, :attrs {:kzfa "N", :stg "psy", :po 2013}, :content nil}]})
  ; missing po definition
  (not (validate {:tag :minors, :attrs nil, :content [{:tag :minor, :attrs {:kzfa "N", :stg "phy"}, :content nil} {:tag :minor, :attrs {:kzfa "N", :stg "bio", :po 2013}, :content nil} {:tag :minor, :attrs {:kzfa "N", :stg "che", :po 2013}, :content nil} {:tag :minor, :attrs {:kzfa "N", :stg "mat", :po 2013} :content nil} {:tag :minor, :attrs {:kzfa "N", :stg "psy", :po 2013}, :content nil}]}))
  ; one minor course is a major course, i.e. kzfa is 'H'
  (not (validate {:tag :minors, :attrs nil, :content [{:tag :minor, :attrs {:kzfa "N", :stg "phy", :po 2013}, :content nil} {:tag :minor, :attrs {:kzfa "N", :stg "bio", :po 2013}, :content nil} {:tag :minor, :attrs {:kzfa "H", :stg "che", :po 2013}, :content nil} {:tag :minor, :attrs {:kzfa "N", :stg "mat", :po 2013}, :content nil} {:tag :minor, :attrs {:kzfa "N", :stg "psy", :po 2013}, :content nil}]}))
  ; mixed: missing and wrong attributes
  (not (validate {:tag :minors, :attrs nil, :content [{:tag :minor, :attrs {:kzfa "N"}, :content nil} {:tag :minor, :attrs {:stg "bio", :po 2013}, :content nil} {:tag :minor, :attrs {:kzfa "N", :stg "che", :po 2013}, :content nil} {:tag :minor, :attrs {:kzfa "N", :stg "mat", :po 2013}, :content nil} {:tag :minor, :attrs {:kzfa "H", :stg "psy", :po 2013}, :content nil}]}))
  ; no attributes in minor tag
  (not (validate {:tag :minors, :attrs nil, :content [{:tag :minor, :attrs {:kzfa "N", :stg "phy", :po 2013}, :content nil} {:tag :minor, :attrs {}, :content nil} ]}))
  ; minors tag has no minor tags
  (not (validate {:tag :minors, :attrs nil, :content []})))

(deftest test-validate-b-tag
  (validate b-tag)
  (validate b-tag-with-regeln))
