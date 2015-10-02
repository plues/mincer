(ns mincer.xml.modules-test
  (:require [clojure.test :refer :all]
            [mincer.xml.modules-test-data :refer :all]
            [mincer.xml.modules :refer :all]))



(def result-abstract-unit {:id "P-IAA-L-BMLS1a"
                           :title  "Grammar 1 (Übung)"
                           :type :m
                           :semester '(1 2)})

(def result-course {:type  :course
                    :id    "ba-aua-KF-2013"
                    :title  "Bachelor Anglistik und Amerikanistik Kernfach PO 2013"})
(def result-module
   (let [pordnr "1"]
     {pordnr {:title  "Module Language Skills 1"
              :course result-course
              :pordnr pordnr
              :abstract-units [result-abstract-unit]}}))

(def result-module3
   (let [pordnr "3"]
     {pordnr {:title  "Module Language Skills II"
              :course result-course
              :pordnr pordnr
              :abstract-units [result-abstract-unit]}}))


(def result-modules (merge result-module result-module3))

(def result-cmu {:modules result-modules})

(deftest test-abstract-unit-parsing
  (is (= result-abstract-unit (tree-to-module-map abstract-unit))))

(deftest test-module-parsing
  ; two testcases one with one without pordnr
  (is (= (tree-to-module-map module result-course) result-module))
  (is (= (tree-to-module-map module2 result-course) nil)))

(deftest test-modules-parsing
  ; two testcases one with one without pordnr
  (is (= result-modules (tree-to-module-map modules result-course))))

(deftest test-course-parsing
  (is (= result-modules (tree-to-module-map course))))

(deftest test-cmu-parsing
  (is (= result-cmu (tree-to-module-map cmu))))


; unit parsing
(def result-session {:type :session :day "tue" :time 1 :duration 2 :rhythm 0})
(def result-session2 {:type :session :day "mon" :time 3 :duration 1 :rhythm 2})
(def result-group {:type :group :sessions [result-session result-session2]})
(def result-abstract-unit-ref {:id "P-Phil-L-BPPKb" :type :abstract-unit-ref})
(def result-unit {:type :unit :id "120281" :title "Aristoteles: Politik (Basisseminar)" :semester [1 3 5] :groups [result-group]
                  :refs [result-abstract-unit-ref]})

(deftest test-parse-session (is (= result-session (tree-to-unit-map session))))
(deftest test-parse-group (is (= result-group (tree-to-unit-map group))))
(deftest test-parse-abstract-unit-ref (is (= result-abstract-unit-ref (tree-to-unit-map abstract-unit-ref))))
(deftest test-parse-unit (is (= result-unit (tree-to-unit-map unit))))
