(ns mincer.data
  (:gen-class)
  (:require
    [clojure.string :refer [join upper-case]]
    [clojure.tools.logging :as log]
    [mincer.module-combinations :refer [traverse-course]]
    [mincer.db :refer [run-on-db setup-db insert! insert-all!
                       abstract-unit-by-key module-by-pordnr
                       load-course-module-map]]))


(declare persist-courses)
(defn store-unit-abstract-unit-semester [db-con unit-id semesters abstract-unit-ref]
  (let [{:keys [id]} (abstract-unit-by-key db-con (:id abstract-unit-ref))]
    (if-not (nil? id)
      (doseq [s semesters]
        (insert! db-con :unit_abstract_unit_semester {:unit_id unit-id
                                                      :abstract_unit_id id
                                                      :semester s})))
      (log/debug "No au for " (:id  abstract-unit-ref))))

(defn store-refs [db-con unit-id refs semesters]
  (doseq [r refs]
    (store-unit-abstract-unit-semester db-con unit-id semesters r)))

(defn session-record [group-id session]
  (assert (= :session (:type session)))
  (assoc (dissoc session :type) :group_id group-id))

(defn store-group [db-con unit-id {:keys [half-semester type sessions]}]
  (assert (= :group type) type)
  (let [group-id (insert! db-con :groups {:unit_id unit-id :half_semester half-semester})
        session-recoreds (map (partial session-record group-id) sessions)]
    (insert-all! db-con :sessions session-recoreds)))

(defn store-unit [db-con {:keys [type id title semester groups refs]}]
  (assert (= :unit type))
  (let [record {:unit_key id :title title}
        unit-id (insert! db-con :units record)]
    (doseq [g groups]
      (store-group db-con unit-id g))
    (store-refs db-con unit-id refs semester)))

(defn persist-units [db-con units]
  (doseq [u units]
    (store-unit db-con u)))

(defn store-stuff [db-con levels modules units]
  (persist-courses db-con levels modules)
  (persist-units db-con units))

(defn store-abstract-unit [db-con module-id {:keys [id title type semester]}]
  (let [record {:key id
                :title title
                :type (name type)
                }
        au-id (insert! db-con :abstract_units record)
        merge-table-fn (fn [sem]
                         (insert! db-con :modules_abstract_units_semesters {:abstract_unit_id au-id
                                                                            :module_id module-id
                                                                            :semester sem}))]
    (doseq [s semester] (merge-table-fn s))))

(defn store-abstract-units [db-con module-id abstract-units]
  (doseq [au abstract-units]
    (when (nil? (abstract-unit-by-key db-con (:id au))) ; abstract unit not yet in the database
      (store-abstract-unit db-con module-id au))))

(defmulti store-child (fn [child & args] (:type child)))

(defmethod store-child :level [{:keys [min max min-cp max-cp name tm art children]} db-con parent-id course-id modules]
  "Insert node into level table in db-con. Returns id of created record."
  (let [record {:parent_id         parent-id
                :min               min
                :max               max
                :min_credit_points min-cp
                :max_credit_points max-cp
                :tm                tm
                :art               art
                :name              name}
        parent-id (insert! db-con :levels record)]
    (doseq [l children]
      (store-child l db-con parent-id course-id modules))
    ; return the id of the created record
    parent-id))

(defmethod store-child :module [{:keys [name cp id pordnr mandatory]} db-con parent-id course-id modules]
  (log/debug "Module " (get modules id))
  (if-let [module-from-db (:id (module-by-pordnr db-con pordnr))]
    module-from-db ; module is already in the database
    (let [{:keys [title abstract-units course key elective-units]} (get modules id)
          record {:level_id       parent-id
                  :mandatory      mandatory
                  :elective_units elective-units
                  :key            key
                  :credit_points  cp; xxx this is nil for some reason
                  :name           name}]
      (log/debug "Title type " (type title))
      (if-not (nil? title) ; NOTE: or use something else to detect a vaild record
        ; merge both module records
        (let [extended-record (merge record {:pordnr pordnr :title title})
              module-id (insert! db-con :modules extended-record)]
          (insert! db-con :course_modules {:course_id course-id :module_id module-id})
          (store-abstract-units db-con module-id abstract-units)
          ; return the id of the created record
          module-id)))))

(defmethod store-child :default [child & args]
  (throw  (IllegalArgumentException. (str (:type child)))))


(defn store-course-module-combination [db-con course-id course-module-map module-combination-id module-combination] ; discard module-combinations that have "empty" modules (i.e. modules without actual units)
  (let [modules (map #(Integer/parseInt (:pordnr %)) module-combination)]
    (if (every? identity (map (fn [m] (contains? course-module-map m)) modules))
      (doseq [m modules]
             (let [record {:course_id course-id
                           :combination_id module-combination-id
                           :module_id (get course-module-map m)}]
               (insert! db-con :course_modules_combinations record)))
      (log/debug "Discarding module combination for course " course-id modules))))

(defn store-course-module-combinations [db-con course course-id]
    (let [course-module-map (load-course-module-map db-con course-id)]
      ; compute module combinations for course and store them to
      ; course_module_combinations
      (doall
        (map-indexed
          (partial store-course-module-combination db-con course-id course-module-map)
          (traverse-course course)))))

(defn store-course [db-con c modules]
  (let [{:keys  [kzfa cp degree course name po children]} c
        params {:degree        degree
                :key           (upper-case (join "-" [degree course kzfa po]))
                :short_name    course
                :kzfa          kzfa ; XXX find a propper name for this
                :name          name
                :credit_points cp
                :po            po}
        parent-id (insert! db-con :courses params)
        levels (map (fn [l] (store-child l db-con nil parent-id modules)) children)]
    (log/debug {:kzfa kzfa :degree degree :course course :name name :po po})
    ; insert course-level/parent-id pairs into course_level table
    (doseq [l levels]
      (insert! db-con :course_levels {:course_id parent-id :level_id l}))
    (store-course-module-combinations db-con c parent-id)))

(defn persist-courses [db-con levels modules]
  (doseq [l levels]
    (store-course db-con l modules)))

(defn persist [levels modules units]
  (run-on-db #(store-stuff % levels modules units)))
