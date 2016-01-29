(ns mincer.quick_check.combinations_test
  (:require
    [clojure.test :refer :all]
    [clojure.test.check :as tc]
    [clojure.test.check.generators :as gen]
    [clojure.test.check.properties :as prop]
    [mincer.module-combinations :refer :all]
    [mincer.xml.tree :refer :all]))

;; Helper
(defn createModule [name id cp]
  {:type :module
    :name name
    :id id
    :cp cp
    :pordnr id
    :mandatory false})

(defn createLevel [min max name min-cp max-cp children]
  {:type :level :min min :max max :name name
    :min-cp min-cp
    :max-cp max-cp
    :tm nil
    :art nil
    :children children})

(defn createCourse [name cp course children]
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
  (createModule "Modul 1" "1" nil))

(def module-2
  (createModule "Modul 2" "2" nil))

(def level-1
  (createLevel 1 2 "Level 1" nil nil [module-1 module-2]))

(def course-1
  (createCourse "Major 1" nil "maj1" [level-1]))

(deftest test-tree-1
    (is (= (traverse-course course-1)
      #{#{module-1} #{module-2} #{module-1 module-2}})))

;; Test 2: Doppelter 1..2-Baum (ohne Pflicht)
(def module-3
  (createModule "Modul 3" "3" nil))

(def module-4
  (createModule "Modul 4" "4" nil))

(def level-2
  (createLevel 1 2 "Level 2" nil nil [module-3 module-4]))

(def level-0
  (createLevel 1 2 "Level 0" nil nil [level-1 level-2]))

(def course-2
  (createCourse "Major 2" nil "maj2" [level-0]))

(deftest test-tree-2
    (is (= (traverse-course course-2)
      #{#{module-1 module-3} #{module-1 module-4} #{module-2 module-3} #{module-2 module-4}})))

;; Test 3: Doppelter 1..2-Baum mit nur drei verschieden Modulen
(def level-3
  (createLevel 1 2 "Level 3" nil nil [module-1 module-4]))

(def level-4
  (createLevel 2 2 "Level 4" nil nil [level-1 level-3]))

(def course-3
  (createCourse "Major 3" nil "maj3" [level-4]))

(deftest test-tree-3
    (is (= (traverse-course course-3)
      #{#{module-1 module-2} #{module-4 module-1} #{module-4 module-2}})))

;; Test 4: Größerer Baum
(def module-5
  (createModule "Modul 5" "5" nil))

(def module-6
  (createModule "Modul 6" "6" nil))

(def level-5
  (createLevel 1 3 "Level 5" nil nil [module-1 module-2 module-3]))

(def level-6
  (createLevel 1 2 "Level 6" nil nil [module-4 module-5 module-6]))

(def level-7
  (createLevel 3 3 "Level 7" nil nil [level-5 level-6]))

(def course-4
  (createCourse "Major 4" nil "maj4" [level-7]))

(deftest test-tree-4
    (is (= (traverse-course course-4)
      #{#{module-1 module-2 module-4} #{module-1 module-2 module-5} #{module-1 module-2 module-6}
        #{module-1 module-3 module-4} #{module-1 module-3 module-5} #{module-1 module-3 module-6}
        #{module-2 module-3 module-4} #{module-2 module-3 module-5} #{module-2 module-3 module-6}
        #{module-1 module-4 module-5} #{module-1 module-4 module-6} #{module-1 module-5 module-6}
        #{module-2 module-4 module-5} #{module-2 module-4 module-6} #{module-2 module-5 module-6}
        #{module-3 module-4 module-5} #{module-3 module-4 module-6} #{module-3 module-5 module-6}})))
