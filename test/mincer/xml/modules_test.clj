(ns mincer.xml.modules-test
  (:require [clojure.test :refer :all]
            [mincer.xml.modules-test-data :refer :all]
            [mincer.xml.modules :refer :all]))



(def result-abstract-unit {:id "P-IAA-L-BMLS1A"
                           :title  "Grammar 1 (Übung)"
                           :type :m
                           :semester '(1 2)})

(def result-module
   (let [pordnr "1"]
     {pordnr {:title  "Module Language Skills 1"
              :pordnr pordnr
              :key "P-IAA-M-BMLS1"
              :elective-units 4
              :abstract-units [result-abstract-unit]}}))

(def result-module3
   (let [pordnr "3"]
     {pordnr {:title  "Module Language Skills II"
              :pordnr pordnr
              :key "P-IAA-M-BMLS1"
              :elective-units 5
              :abstract-units [result-abstract-unit]}}))

(def result-module4
   (let [pordnr "4"]
     {pordnr {:title "Module Language Skills II"
              :pordnr pordnr
              :key "P-IAA-M-BMLS1"
              :elective-units 0
              :abstract-units [result-abstract-unit]}}))


(def result-modules {:modules (merge result-module result-module3)})

(deftest test-abstract-unit-parsing
  (is (= result-abstract-unit (tree-to-module-map abstract-unit))))

(deftest test-module-parsing
  ; two testcases one with one without pordnr
  (is (= (tree-to-module-map module) result-module))
  (is (= (tree-to-module-map module2) nil))
  (is (= (tree-to-module-map module4) result-module4)) )

(deftest test-modules-parsing
  ; two testcases one with one without pordnr
  (is (= result-modules (tree-to-module-map modules))))

; unit parsing
(def result-session {:type :session :day "tue" :time 1 :duration 2 :rhythm 0 :tentative false})
(def result-session2 {:type :session :day "mon" :time 3 :duration 1 :rhythm 2 :tentative false})
(def result-session3 {:type :session :day "mon" :time 3 :duration 1 :rhythm 3 :tentative false})
(def result-group {:type :group
                   :half-semester 2
                   :sessions [result-session result-session2 result-session3]})
(def result-abstract-unit-ref {:id "P-PHIL-L-BPPKB" :type :abstract-unit-ref})
(def result-unit {:type :unit
                  :id "120281"
                  :title "Aristoteles: Politik (Basisseminar)"
                  :semester [1 3 5]
                  :groups [result-group]
                  :refs [result-abstract-unit-ref]})

(def result-unit2 {:type :unit
                   :id "120282"
                   :title "Aristoteles: Politik (Basisseminar)"
                   :semester [1 3 5]
                   :groups [result-group]
                  :refs []})

(deftest test-parse-session (is (= result-session (tree-to-unit-map session))))
(deftest test-parse-group (is (= result-group (tree-to-unit-map group))))
(deftest test-parse-abstract-unit-ref (is (= result-abstract-unit-ref (tree-to-unit-map abstract-unit-ref))))
(deftest test-parse-unit (is (= result-unit (tree-to-unit-map unit))))
(deftest test-parse-unit2 (is (= result-unit2 (tree-to-unit-map unit2))))


(def tentative-session {:type :session :day "mon" :time 3 :duration 1 :rhythm 3 :tentative true})
(deftest test-parse-tentative-session (is (= tentative-session (tree-to-unit-map session4))))
