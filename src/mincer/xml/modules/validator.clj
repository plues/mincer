(ns mincer.xml.modules.validator
  (:gen-class)
  (:require
    [clojure.tools.logging :as log]
    [clojure.set :refer [difference]]))

(def required-attributes {:abstract-unit     #{:id :title :type :semester}
                          :abstract-unit-ref #{:id}
                          :course            #{:id :title}
                          :module            #{:id :title :pordnr :elective-units}
                          :session           #{:day :time :duration :rhythm}
                          :unit              #{:id :title :semester}})


(defn- no-content [tag] (into {} (filter (fn [[k v]] (not (= :content k))) tag)))
(defn- attr-names [tag] (-> tag :attrs keys set))


(defn- check-tag [exp tag]
  (if-not (nil? (:tag tag))
    (assert (= exp (:tag tag)) (str "Expected " exp " tag, but was " (no-content tag)))
    (throw (AssertionError. (str "Expected " exp " tag, but was non-tag " tag)))))


(defn- check-attributes [name tag]
  (let [attrs (attr-names tag)
        missing (difference (get required-attributes name) attrs)]
    (assert (= 0 (count missing))
            (str name " tag " tag " missing required attributes " missing))))


(defn check-node
  ([node-name next-fn] (check-node node-name node-name next-fn))
  ([node-name attr-name next-fn] (fn [elem]
                                   (check-tag node-name elem)
                                   (check-attributes attr-name elem)
                                   (when-not (nil? next-fn)
                                     (doall (map next-fn (:content elem)))))))


(def validate-abstract-unit (check-node :abstract-unit nil))
(def validate-module (check-node :module validate-abstract-unit))
(def validate-modules (check-node :modules validate-module))
(def validate-course (check-node :course validate-modules))
(def validate-course-module-units (check-node :course-module-units validate-course))

(def validate-session (check-node :session nil))
(def validate-group (check-node :group validate-session))
(def validate-abstract-unit-ref (check-node :abstract-unit :abstract-unit-ref nil))


(defn validate-unit [unit]
  (check-tag :unit unit)
  (check-attributes :unit unit)
  (doall (map (fn [x]
         (let [tag (:tag x)]
           (cond
                 (= :group tag) (validate-group x)
                 (= :abstract-unit tag) (validate-abstract-unit-ref x)
                 :else (assert false (str "Unexpected element " x))))) (:content unit))))


(def validate-units (check-node :units validate-unit))


(defn validate [doc]
  (check-tag :data doc)
  (doall (map (fn [x]
         (let [tag (:tag x)]
           (cond
                 (= :course-module-units tag) (validate-course-module-units x)
                 (= :units tag) (validate-units x)
                 :else (assert false (str "Unexpected element " x))))) (:content doc)))
  (log/info "input validation done"))
