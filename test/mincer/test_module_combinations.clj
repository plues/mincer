(ns mincer.test-module-combinations ; move to test-module-tree
  (:require
    [clojure.xml :as xml]
    [clojure.test :refer :all]
    [mincer.xml.tree :as tree]
    [mincer.module-combinations :refer :all]))

(defn parse [str] (xml/parse (java.io.ByteArrayInputStream. (.getBytes str))))

(def course1 (tree/parse-tree (parse "<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<b abschl=\"bk\" stg=\"cs\" kzfa=\"H\" cp=\"20\" pversion=\"2013\" name=\"Bachelor Informatik\">
    <l name=\"Grundlagenmodule\" min-cp=\"10\" max-cp=\"10\">
        <m cp=\"10\" name=\"M1\" pordnr=\"1\" />
    </l>
    <l name=\"Grundlagenmodule\" min-cp=\"10\" max-cp=\"10\">
        <m cp=\"10\" name=\"M2\" pordnr=\"2\" />
   </l>
</b>")))


(def course2 (tree/parse-tree (parse "<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<b abschl=\"bk\" stg=\"cs\" kzfa=\"H\" cp=\"20\" pversion=\"2013\" name=\"Bachelor Informatik\">
    <l name=\"Grundlagenmodule\" min-cp=\"10\" max-cp=\"10\">
        <m cp=\"10\" name=\"M1\" pordnr=\"1\" />
    </l>
    <l name=\"Grundlagenmodule\" min-cp=\"10\" max-cp=\"10\">
        <m cp=\"10\" name=\"M1\" pordnr=\"1\" />
   </l>
</b>")))


(deftest test-course1-has-one-combination
  (let [mc (traverse-course course1)]
    ; there is 1 module combination
    (is (= 1 (count mc)))
    ; combination contains two modules
    (map #((is (= 2 (count %)))) mc)))

(deftest test-course2-has-one-combination
  (let [mc (traverse-course course2)]
    ; there are no combinations, because levels share a module
    (is (= 0 (count mc)))))
