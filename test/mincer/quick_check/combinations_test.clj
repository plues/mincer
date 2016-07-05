(ns mincer.quick_check.combinations_test
  (:require
    [clojure.test :refer :all]
    [mincer.module-combinations :refer :all]
    [mincer.xml.tree :refer :all]))

;; Helper
(defn create-module [name id cp]
  {:type :module
    :name name
    :id id
    :cp cp
    :pordnr id
    :mandatory false})

(defn create-level [min max name min-cp max-cp children]
  {:type :level :min min :max max :name name
    :min-cp min-cp
    :max-cp max-cp
    :tm nil
    :art nil
    :children children})

(defn create-course [name cp course children]
  {:type :course
   :degree "bk"
   :kzfa "H"
   :name name
   :po 2011
   :cp cp
   :course course
   :children children})

;; Test 1: Einfacher 1..2-Baum (ohne Pflicht)
(def module-1
  (create-module "Modul 1" "1" nil))

(def module-2
  (create-module "Modul 2" "2" nil))

(def level-1
  (create-level 1 2 "Level 1" nil nil [module-1 module-2]))

(def course-1
  (create-course "Major 1" nil "maj1" [level-1]))

(deftest test-tree-1
    (is (= (traverse-course course-1)
      #{#{module-1} #{module-2} #{module-1 module-2}})))

;; Test 2: Doppelter 1..2-Baum (ohne Pflicht)
(def module-3
  (create-module "Modul 3" "3" nil))

(def module-4
  (create-module "Modul 4" "4" nil))

(def level-2
  (create-level 1 2 "Level 2" nil nil [module-3 module-4]))

(def level-0
  (create-level 1 2 "Level 0" nil nil [level-1 level-2]))

(def course-2
  (create-course "Major 2" nil "maj2" [level-0]))

(deftest test-tree-2
    (is (= (traverse-course course-2)
      #{#{module-1 module-3} #{module-1 module-4} #{module-2 module-3} #{module-2 module-4}})))

;; Test 3: Doppelter 1..2-Baum mit nur drei verschieden Modulen
(def level-3
  (create-level 1 2 "Level 3" nil nil [module-1 module-4]))

(def level-4
  (create-level 2 2 "Level 4" nil nil [level-1 level-3]))

(def course-3
  (create-course "Major 3" nil "maj3" [level-4]))

(deftest test-tree-3
    (is (= (traverse-course course-3)
      #{#{module-1 module-2} #{module-4 module-1} #{module-4 module-2}})))

;; Test 4: Größerer Baum
(def module-5
  (create-module "Modul 5" "5" nil))

(def module-6
  (create-module "Modul 6" "6" nil))

(def level-5
  (create-level 1 3 "Level 5" nil nil [module-1 module-2 module-3]))

(def level-6
  (create-level 1 2 "Level 6" nil nil [module-4 module-5 module-6]))

(def level-7
  (create-level 3 3 "Level 7" nil nil [level-5 level-6]))

(def course-4
  (create-course "Major 4" nil "maj4" [level-7]))

(deftest test-tree-4
    (is (= (traverse-course course-4)
      #{#{module-1 module-2 module-4} #{module-1 module-2 module-5} #{module-1 module-2 module-6}
        #{module-1 module-3 module-4} #{module-1 module-3 module-5} #{module-1 module-3 module-6}
        #{module-2 module-3 module-4} #{module-2 module-3 module-5} #{module-2 module-3 module-6}
        #{module-1 module-4 module-5} #{module-1 module-4 module-6} #{module-1 module-5 module-6}
        #{module-2 module-4 module-5} #{module-2 module-4 module-6} #{module-2 module-5 module-6}
        #{module-3 module-4 module-5} #{module-3 module-4 module-6} #{module-3 module-5 module-6}})))

;; Test 5: Einfacher 1..2-Baum (mit CPs)
(def module-7
  (create-module "Modul 7" "7" 5))

(def module-8
  (create-module "Modul 8" "8" 5))

(def module-9
  (create-module "Modul 9" "9" 5))

(def module-10
  (create-module "Modul 10" "10" 5))

(def level-9
  (create-level nil nil "Level 9" 0 10 [module-7 module-8]))

(def level-10
  (create-level nil nil "Level 10" 5 10 [module-9 module-10]))

(def level-8
  (create-level nil nil "Level 8" 10 15 [level-9 level-10]))

(def course-5
  (create-course "Major 5" 15 "maj5" [level-8]))

(deftest test-tree-5
    (is (= (traverse-course course-5)
      #{#{module-9 module-10} #{module-7 module-9 module-10} #{module-8 module-9 module-10}
      #{module-7 module-8 module-9} #{module-7 module-8 module-10} #{module-8 module-10}
      #{module-8 module-9} #{module-7 module-10} #{module-7 module-9}})))
