(ns mincer.quick_check.combinations_test
  (:require
    [clojure.test :refer :all]
    [clojure.test.check :as tc]
    [clojure.test.check.generators :as gen]
    [clojure.test.check.properties :as prop]
    [mincer.module-combinations :refer :all]
    [mincer.xml.tree :refer :all]))

;; Test 1: Einfacher 1..2-Baum (ohne Pflicht)
(def module-1 {:type :module
              :name "Modul 1"
              :id "1"
              :cp nil
              :pordnr "1"
              :mandatory false})

(def module-2 {:type :module
              :name "Modul 2"
              :id "2"
              :cp nil
              :pordnr "2"
              :mandatory false})

(def level-1 {:type :level :min 1 :max 2 :name "Level 1"
            :min-cp nil
            :max-cp nil
            :tm nil
            :art nil
            :children [module-1 module-2]})

(def course-1 {:type :course
             :degree "bk"
             :kzfa "H"
             :name "Major"
             :po 2011
             :cp nil
             :course "maj"
             :children [level-1]})

(deftest test-tree-1
    (is (= (traverse-course course-1)
      #{#{module-1} #{module-2} #{module-1 module-2}})))

;; Test 2: Doppelter 1..2-Baum (ohne Pflicht)
(def module-3 {:type :module
              :name "Modul 3"
              :id "3"
              :cp nil
              :pordnr "3"
              :mandatory false})

(def module-4 {:type :module
              :name "Modul 4"
              :id "4"
              :cp nil
              :pordnr "4"
              :mandatory false})

(def level-2 {:type :level :min 1 :max 2 :name "Level 2"
            :min-cp nil
            :max-cp nil
            :tm nil
            :art nil
            :children [module-3 module-4]})

(def level-0 {:type :level :min 1 :max 2 :name "Level 0"
            :min-cp nil
            :max-cp nil
            :tm nil
            :art nil
            :children [level-1 level-2]})

(def course-2 {:type :course
             :degree "bk"
             :kzfa "H"
             :name "Major 2"
             :po 2011
             :cp nil
             :course "maj"
             :children [level-0]})

(deftest test-tree-2
    (is (= (traverse-course course-2)
      #{#{module-1 module-3} #{module-1 module-4} #{module-2 module-3} #{module-2 module-4}})))

;; Test 3: Doppelter 1..2-Baum mit nur drei verschieden Modulen
(def level-3 {:type :level :min 1 :max 2 :name "Level 3"
            :min-cp nil
            :max-cp nil
            :tm nil
            :art nil
            :children [module-1 module-4]})

(def level-4 {:type :level :min 2 :max 2 :name "Level 4"
            :min-cp nil
            :max-cp nil
            :tm nil
            :art nil
            :children [level-1 level-3]})

(def course-3 {:type :course
             :degree "bk"
             :kzfa "H"
             :name "Major 3"
             :po 2011
             :cp nil
             :course "maj"
             :children [level-4]})

(deftest test-tree-3
    (is (= (traverse-course course-3)
      #{#{module-1 module-2} #{module-4 module-1} #{module-4 module-2}})))

;; Test 4: Größerer Baum
(def module-5 {:type :module
              :name "Modul 5"
              :id "5"
              :cp nil
              :pordnr "5"
              :mandatory false})

(def module-6 {:type :module
              :name "Modul 6"
              :id "6"
              :cp nil
              :pordnr "6"
              :mandatory false})

(def level-5 {:type :level :min 1 :max 3 :name "Level 5"
            :min-cp nil
            :max-cp nil
            :tm nil
            :art nil
            :children [module-1 module-2 module-3]})

(def level-6 {:type :level :min 1 :max 2 :name "Level 6"
            :min-cp nil
            :max-cp nil
            :tm nil
            :art nil
            :children [module-4 module-5 module-6]})

(def level-7 {:type :level :min 3 :max 3 :name "Level 7"
            :min-cp nil
            :max-cp nil
            :tm nil
            :art nil
            :children [level-5 level-6]})

(def course-4 {:type :course
             :degree "bk"
             :kzfa "H"
             :name "Major 4"
             :po 2011
             :cp nil
             :course "maj"
             :children [level-7]})

(deftest test-tree-4
    (is (= (traverse-course course-4)
      #{#{module-1 module-2 module-4} #{module-1 module-2 module-5} #{module-1 module-2 module-6}
        #{module-1 module-3 module-4} #{module-1 module-3 module-5} #{module-1 module-3 module-6}
        #{module-2 module-3 module-4} #{module-2 module-3 module-5} #{module-2 module-3 module-6}
        #{module-1 module-4 module-5} #{module-1 module-4 module-6} #{module-1 module-5 module-6}
        #{module-2 module-4 module-5} #{module-2 module-4 module-6} #{module-2 module-5 module-6}
        #{module-3 module-4 module-5} #{module-3 module-4 module-6} #{module-3 module-5 module-6}})))