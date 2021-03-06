(ns mincer.data
  (:gen-class)
  (:require
    [clojure.string :refer [join upper-case]]
    [clojure.tools.logging :as log]
    [mincer.module-combinations :refer [traverse-course]]
    [mincer.bitvector :refer [bitvector set-bit! get-bytes get-bit]]
    [mincer.db :refer [abstract-unit-by-key insert!
                       insert-all! query-course load-course-module-map module-by-pordnr
                       run-on-db setup-db ]]))

(declare persist-courses)
(defn store-unit-abstract-unit [db-con unit-id abstract-unit-ref]
  (let [{:keys [id]} (abstract-unit-by-key db-con (:id abstract-unit-ref))]
    (if (nil? id)
      (log/trace "No au for " (:id  abstract-unit-ref))
      {:unit_id unit-id :abstract_unit_id id})))

(defn store-refs [db-con unit-id refs]
  (insert-all! db-con :unit_abstract_unit
               (remove nil? (map #(store-unit-abstract-unit db-con unit-id %) refs))))

(defn store-semesters [db-con unit-id semesters]
  (insert-all! db-con :unit_semester (map
                                       (fn [s] {:unit_id unit-id :semester s})
                                       semesters)))

(defn session-record [group-id session]
  (assert (= :session (:type session)))
  (assoc (dissoc session :type) :group_id group-id))

(defn store-group [db-con unit-id {:keys [half-semester type sessions]}]
  (assert (= :group type) type)
  (let [group-id (insert! db-con :groups {:unit_id unit-id :half_semester half-semester})
        session-records (map #(session-record group-id %) sessions)]
    (insert-all! db-con :sessions session-records)))

(defn store-unit [db-con {:keys [type id title semester groups refs]}]
  (assert (= :unit type))
  (let [record {:unit_key id :title title}
        unit-id (insert! db-con :units record)]
    (doseq [g groups]
      (store-group db-con unit-id g))
    (store-refs db-con unit-id refs)
    (store-semesters db-con unit-id semester)))

(defn persist-units [db-con units]
  (doseq [u units]
    (store-unit db-con u)))

(defn persist-metadata [db-con md]
  (insert-all! db-con :info (map (fn [[k v]] {:key (name k) :value v}) md)))

(defn store-major-minor-pairs [db-con minor-dict major-course-id]
  (let [record {:course_id       major-course-id
                :minor_course_id (query-course db-con minor-dict "N")}]
    (if (nil? (record :minor_course_id)) 
      (log/warn "Minor course does not exist with attributes " minor-dict) 
      (insert! db-con :minors record))))

(defn store-minors [db-con dict]
  (doseq [major-dict dict]
    (let [major-course-id   (query-course db-con major-dict "H")
          minor-dict        (major-dict :minors)
          minors            (if-not (nil? minor-dict) (minor-dict :children) nil)]
      (if (nil? major-course-id) 
        (log/warn "Major course does not exist with attributes " major-dict)
        (when (not (nil? minors)) (doall (map #(store-major-minor-pairs db-con % major-course-id) minors)))))))

(defn store-stuff [db-con levels modules units]
  (let [minors-to-be-stored (remove nil? (persist-courses db-con levels modules))]
    (persist-units db-con units)
    (persist-metadata db-con (:info levels))
    ; afterwards insert major/minor course pairs to :minors database table (we need the course ids)
    (store-minors db-con minors-to-be-stored)))

(defn store-abstract-unit [db-con module-id {:keys [id title type]}]
  (let [record {:key id
                :title title}]
        (insert! db-con :abstract_units record)))

(defn store-abstract-units [db-con module-id abstract-units]
  (doseq [au abstract-units]
    (let [au-rec (abstract-unit-by-key db-con (:id au))
          au-id (if (nil? au-rec)
                  (store-abstract-unit db-con module-id au) ; abstract unit not yet in the database
                  (:id au-rec))] ; abstract unit in the database
      ; link au with module and semester
      (insert-all! db-con
                   :modules_abstract_units_semesters
                   (map (fn [s] {:abstract_unit_id au-id
                           :module_id module-id
                           :semester s})
                        (:semester au)))
      ; link au with module and type
      (insert! db-con :modules_abstract_units_types {:abstract_unit_id au-id
                                                     :module_id module-id
                                                     :type (-> au :type name)}))))

(defmulti store-child (fn [child & args] (:type child)))

; "Insert node into level table in db-con. Returns id of created record."
(defmethod store-child :level [{:keys [min max min-cp max-cp name tm art children]} db-con parent-id course-id modules]
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

(defn store-module [db-con modules id pordnr]
  (let [{:keys [title abstract-units course key elective-units bundled]} (get modules id)
        record {:elective_units elective-units
                :bundled        bundled
                :key            key}]
    (log/trace "Title type " (type title))
    (if-not (nil? title) ; NOTE: or use something else to detect a valid record
      ; merge both module records
      (let [extended-record (merge record {:pordnr pordnr :title title})
            module-id (insert! db-con :modules extended-record)]
        (store-abstract-units db-con module-id abstract-units)
        ; return the id of the created record
        module-id))))


(defmethod store-child :module [{:keys [name cp id pordnr mandatory]} db-con parent-id course-id modules]
  (log/trace "Module " (get modules id))
  ; if module is already in database we assume that this is another path to it
  (let [module-id (if-let [module-from-db (module-by-pordnr db-con pordnr)]
                            (:id module-from-db) 
                            (store-module db-con modules id pordnr))]
    ; if the module exists (was found or was created)
    (when-not (nil? module-id) 
      ; create module level entry 
      ; link module and tree
      (log/trace "store-child :module module-id: " module-id " course-id: " course-id)
      (insert! db-con :module_levels {:module_id     module-id    
                                      :level_id      parent-id
                                      :course_id     course-id ; shortcut to the tree's root
                                      :mandatory     mandatory
                                      :name          name
                                      :credit_points cp}))
      ; return the id of the created record
      module-id))


(defmethod store-child :default [child & args]
  (throw  (IllegalArgumentException. (str (:type child)))))

(defn store-single-module-combination [db-con course-id
                                       course-module-map modules]
  (let [bv (bitvector)]
    (doseq [m modules]
      (let [idx (get course-module-map m)]
        (when (get-bit bv idx)
          (log/error "Bit" idx "was already set"))
        (set-bit! bv idx)))
    (insert!
      db-con :course_modules_combinations {:course_id course-id
                                           :combination (get-bytes bv)})))


(defn store-course-module-combination [db-con course-id
                                       course-module-map module-combination] 
  ; discard module-combinations that have "empty" modules (i.e. modules without actual units)
  (let [modules (map #(Integer/parseInt (:pordnr %)) module-combination)]
    (if (every? identity (map (fn [m] (contains? course-module-map m)) modules))

      (store-single-module-combination db-con course-id course-module-map modules)

      (log/trace "Discarding module combination for course " course-id modules))))


(defn store-course-module-combinations [db-con course course-id]
    (let [; course-module-map maps from module pordnr to database id for all
          ; modules in a course
          course-module-map (load-course-module-map db-con course-id)]
      ; compute module combinations for course and store them to
      ; the course_module_combinations table
      (doall
        (map
          #(store-course-module-combination db-con course-id course-module-map %)
          (traverse-course course)))))

; store courses and return a dict of minor courses if given
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
        minors (first (filter #(= :minors (% :type)) children)) ; filter minors tag
        levels (map (fn [l] (store-child l db-con nil parent-id modules)) (filter #(not (= :minors (% :type))) children))]
    (log/trace {:kzfa kzfa :degree degree :course course :name name :po po})
    ; insert course-level/parent-id pairs into course_level table
    (dorun (insert-all! db-con :course_levels
                        (map
                          (fn [l] {:course_id parent-id :level_id l})
                          levels)))
    (store-course-module-combinations db-con c parent-id)
    ; return dict of major course with its minor courses to insert them afterwards
    (when (= kzfa "H") {:type :major :short_name course :po po :minors minors})))

; store the courses and return all minor courses defined by minors-Tag
(defn persist-courses [db-con levels modules]
  (doall (for [l (:levels levels)] (store-course db-con l modules))))

(defn persist [levels modules units]
  (run-on-db #(store-stuff % levels modules units)))
