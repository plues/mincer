(ns mincer.xml.modules.validator
  (:gen-class)
  (:require
    [clojure.tools.logging :as log]
    [mincer.xml.util :refer [check-node check-tag check-attributes]]))

(def required-attributes {:abstract-unit     #{:id :title :type :semester}
                          :abstract-unit-ref #{:id}
                          :course            #{:id :title}
                          :module            #{:id :title :pordnr :elective-units}
                          :session           #{:day :time :duration :rhythm}
                          :unit              #{:id :title :semester}})


(def validate-abstract-unit       (check-node :abstract-unit                    (:abstract-unit required-attributes) nil))
(def validate-module              (check-node :module                           (:module required-attributes) validate-abstract-unit))
(def validate-modules             (check-node :modules                          (:modules required-attributes) validate-module))
(def validate-course              (check-node :course                           (:course required-attributes) validate-modules))
(def validate-course-module-units (check-node :course-module-units              (:course-module-units required-attributes) validate-course))

(def validate-session             (check-node :session                          (:session required-attributes) nil))
(def validate-group               (check-node :group                            (:group required-attributes) validate-session))
(def validate-abstract-unit-ref   (check-node :abstract-unit :abstract-unit-ref (:abstract-unit-ref required-attributes) nil))


(defn validate-unit [unit]
  (check-tag :unit unit)
  (check-attributes :unit unit (:unit required-attributes))
  (doall (map (fn [x]
         (let [tag (:tag x)]
           (cond
                 (= :group tag) (validate-group x)
                 (= :abstract-unit tag) (validate-abstract-unit-ref x)
                 :else (assert false (str "Unexpected element " x))))) (:content unit))))


(def validate-units (check-node :units (:units required-attributes) validate-unit))


(defn validate [doc]
  (check-tag :data doc)
  (doall (map (fn [x]
         (let [tag (:tag x)]
           (cond
                 (= :course-module-units tag) (validate-course-module-units x)
                 (= :units tag) (validate-units x)
                 :else (assert false (str "Unexpected element " x))))) (:content doc)))
  (log/info "input validation done"))
