(ns mincer.data
  (:gen-class)
  (:require
    [clojure.java.jdbc :as jdbc]
    [clojure.java.jdbc :refer [with-db-transaction]]
    [clojure.string :refer [join upper-case]]
    [clojure.tools.logging :as log]
    [mincer.module-combinations :refer [traverse-course]]))

(def mincer-version "0.1.0-SNAPSHOT") ; updated with bumpversion
(defn setup-db [db-con]
  ; maybe use DDL for schema
  (jdbc/db-do-commands  db-con (jdbc/create-table-ddl :info
                                                     [:key :string "NOT NULL"]
                                                     [:value :string "DEFAULT ''" "NOT NULL"])
                               "CREATE UNIQUE INDEX info_key ON info(key)"
                               "PRAGMA foreign_keys = ON"
                               (jdbc/create-table-ddl :levels
                                                     [:id :integer "PRIMARY KEY" "AUTOINCREMENT"]
                                                     [:name :string "NOT NULL"]
                                                     [:tm :string]
                                                     [:art :string]
                                                     [:min :integer] ; NOTE can be nil if tm and art are set or if we are using credit points
                                                     [:max :integer] ; NOTE can be nil if tm and art are set or if we are using credit points
                                                     [:min_credit_points :integer :default "NULL"] ; NOTE can be nil
                                                     [:max_credit_points :integer :default "NULL"] ; NOTE can be nil
                                                     [:parent_id :integer "REFERENCES levels"]
                                                     [:created_at :datetime :default :current_timestamp]
                                                     [:updated_at :datetime :default :current_timestamp])
                               "CREATE INDEX parent_level ON levels(parent_id)"

                               (jdbc/create-table-ddl :courses
                                                      [:id :integer "PRIMARY KEY" "AUTOINCREMENT"]
                                                      [:key :string "NOT NULL"]
                                                      [:degree :string "NOT NULL"]
                                                      [:short_name :string "NOT NULL"]
                                                      [:name :string "NOT NULL"]
                                                      [:kzfa :string "NOT NULL"]
                                                      [:po :integer]
                                                      [:credit_points :integer :default "NULL"]
                                                      [:created_at :datetime :default :current_timestamp]
                                                      [:updated_at :datetime :default :current_timestamp])
                               "CREATE INDEX course_key ON courses(key)"

                               (jdbc/create-table-ddl :course_levels
                                                      [:course_id "NOT NULL" "REFERENCES courses"]
                                                      [:level_id "NOT NULL" "REFERENCES levels"])
                               "CREATE INDEX course_level_course ON course_levels(course_id)"
                               "CREATE INDEX course_level_level ON course_levels(level_id)"

                               (jdbc/create-table-ddl :course_modules
                                                      [:course_id "NOT NULL" "REFERENCES courses"]
                                                      [:module_id "NOT NULL" "REFERENCES modules"])
                               "CREATE INDEX course_module_course ON course_modules(course_id)"
                               "CREATE INDEX course_module_module ON course_modules(module_id)"

                               (jdbc/create-table-ddl :modules
                                                      [:id :integer "PRIMARY KEY" "AUTOINCREMENT"]
                                                      [:level_id :integer "REFERENCES levels"]
                                                      [:key :string "NOT NULL"]
                                                      ; XXX consider discarding one of both
                                                      [:name :string "NOT NULL"]
                                                      [:title  :string]
                                                      [:pordnr :integer]
                                                      [:mandatory :boolean]
                                                      [:elective_units :integer]
                                                      [:credit_points :integer :default "NULL"]
                                                      ; XXX do we a direct link to the course?
                                                      [:created_at :datetime :default :current_timestamp]
                                                      [:updated_at :datetime :default :current_timestamp])

                               (jdbc/create-table-ddl :course_modules_combinations
                                                      [:course_id "REFERENCES courses"]
                                                      [:module_id "REFERENCES modules"]
                                                      [:combination_id "INTEGER"]) ; unique for each course; represents each combination in a course
                               "CREATE INDEX course_modules_combinations_course ON course_modules_combinations(course_id)"
                               "CREATE INDEX course_modules_combinations_module ON course_modules_combinations(module_id)"

                               (jdbc/create-table-ddl :abstract_units
                                                      [:id :integer "PRIMARY KEY" "AUTOINCREMENT"]
                                                      [:key :string "NOT NULL"]
                                                      [:title :string "NOT NULL"]
                                                      [:type :string "NOT NULL"]
                                                      [:created_at :datetime :default :current_timestamp]
                                                      [:updated_at :datetime :default :current_timestamp])
                               "CREATE INDEX abstract_unit_key ON abstract_units(key)"

                               (jdbc/create-table-ddl :modules_abstract_units_semesters
                                                      [:abstract_unit_id :int "NOT NULL" "REFERENCES abstract_units"]
                                                      [:module_id :int "NOT NULL" "REFERENCES modules"]
                                                      [:semester :int "NOT NULL"])
                               "CREATE INDEX modules_abstract_units_semesters_au_id ON modules_abstract_units_semesters(abstract_unit_id)"
                               "CREATE INDEX modules_abstract_units_semesters_module_id ON modules_abstract_units_semesters(module_id)"

                               (jdbc/create-table-ddl :unit_abstract_unit_semester
                                                      [:unit_id :int "NOT NULL" "REFERENCES unit"]
                                                      [:abstract_unit_id :int "NOT NULL" "REFERENCES abstract_unit"]
                                                      [:semester :int "NOT NULL"])
                               "CREATE INDEX unit_abstract_unit_semester_unit_id ON unit_abstract_unit_semester(unit_id)"
                               "CREATE INDEX unit_abstract_unit_semester_abstract_unit_id ON unit_abstract_unit_semester(abstract_unit_id)"

                               (jdbc/create-table-ddl :units
                                                      [:id :integer "PRIMARY KEY" "AUTOINCREMENT"]
                                                      [:unit_key :string "NOT NULL"]
                                                      [:title :string "NOT NULL"]
                                                      [:created_at :datetime :default :current_timestamp]
                                                      [:updated_at :datetime :default :current_timestamp])
                               "CREATE INDEX unit_key ON units(unit_key)"

                                (jdbc/create-table-ddl :groups
                                                      [:id :integer "PRIMARY KEY" "AUTOINCREMENT"]
                                                      [:unit_id :int "NOT NULL" "REFERENCES units"]
                                                      [:half_semester :integer "NOT NULL"]
                                                      [:created_at :datetime :default :current_timestamp]
                                                      [:updated_at :datetime :default :current_timestamp])
                               "CREATE INDEX group_unit_id ON groups(unit_id)"

                                 (jdbc/create-table-ddl :sessions
                                                      [:id :integer "PRIMARY KEY" "AUTOINCREMENT"]
                                                      [:group_id :integer "NOT NULL" "REFERENCES groups"]
                                                      [:day :string "NOT NULL"]
                                                      [:time :integer "NOT NULL"]
                                                      [:duration :integer "NOT NULL"]
                                                      [:rhythm :integer "NOT NULL"]
                                                      [:created_at :datetime :default :current_timestamp]
                                                      [:updated_at :datetime :default :current_timestamp])
                               "CREATE INDEX session_group_id ON sessions(group_id)"

                                (jdbc/create-table-ddl :log
                                                      [:session_id :integer "NOT NULL" "REFERENCES sessions"]
                                                      [:src_day :string]
                                                      [:src_time :integer]
                                                      [:target_day :string]
                                                      [:target_time :integer]
                                                      [:created_at :datetime :default :current_timestamp])
                               "CREATE INDEX log_session_id ON log(session_id)")

  (jdbc/insert! db-con :info {:key "schema_version"
                              :value (str "v3.0")})
  (jdbc/insert! db-con :info {:key "generator"
                              :value (str "mincer" "-" mincer-version)}))

(defn insert! [db-con table rec]
  (log/debug "Saving to" table rec)
  ((keyword "last_insert_rowid()")
   (first (jdbc/insert! db-con table rec))))

(defn insert-all! [db-con table recs]
  (log/debug "Saving to" table recs)
  (apply jdbc/insert! db-con table recs))
(defn run-on-db [func]
    (let [database (.getPath (java.io.File/createTempFile "mince" ".sqlite3"))
          db-spec {:classname   "org.sqlite.JDBC"
                   :subprotocol "sqlite"
                   :subname     database}]
      (log/debug "Database " database)
      (with-db-transaction [db-con db-spec]
        (setup-db db-con)
        (func db-con))
      ; return path to generated DB
      database))


(declare persist-courses)
(defn store-unit-abstract-unit-semester [db-con unit-id semesters abstract-unit-ref]
  (let [[{:keys [id]}] (jdbc/query db-con
                          ["SELECT id FROM abstract_units WHERE key = ?"
                           (:id  abstract-unit-ref)])]
    (if-not (nil? id)
      (mapv (fn [s] (insert! db-con :unit_abstract_unit_semester {:unit_id unit-id :abstract_unit_id id :semester s})) semesters)
      (log/debug "No au for " (:id  abstract-unit-ref)))))

(defn store-refs [db-con unit-id refs semesters]
  (mapv
    (partial store-unit-abstract-unit-semester db-con unit-id semesters) refs))

(defn store-session [db-con group-id session]
  (assert (= :session (:type session)))
  (insert! db-con :sessions (assoc (dissoc session :type) :group_id group-id)))

(defn store-group [db-con unit-id {:keys [half-semester type sessions]}]
  (assert (= :group type) type)
  (let [group-id (insert! db-con :groups {:unit_id unit-id :half_semester half-semester})]
    (mapv (partial store-session db-con group-id) sessions)))

(defn store-unit [db-con {:keys [type id title semester groups refs]}]
  (assert (= :unit type))
  (let [record {:unit_key id :title title}
        unit-id (insert! db-con :units record)]
    (mapv (partial store-group db-con unit-id) groups)
    (store-refs db-con unit-id refs semester)))

(defn persist-units [db-con units]
  (mapv (partial store-unit db-con) units))

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
    (mapv merge-table-fn semester)))

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
    (doall (map (fn [l] (store-child l db-con parent-id course-id modules)) children))
    ; return the id of the created record
    parent-id))

(defmethod store-child :module [{:keys [name cp id pordnr mandatory]} db-con parent-id course-id modules]
  (log/debug "Module " (get modules id))
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
        (mapv (partial store-abstract-unit db-con module-id) abstract-units)
        ; return the id of the created record
        module-id))))

(defmethod store-child :default [child & args]
  (throw  (IllegalArgumentException. (str (:type child)))))

(defn load-course-module-map [db-con course-id]
  (let [sql "SELECT modules.pordnr, modules.id FROM course_modules JOIN modules ON course_modules.module_id WHERE course_id = ?;"
        res (jdbc/query db-con [sql course-id])]
    (into {} (map (fn [{:keys [pordnr id]}] [pordnr id]) res))))

(defn store-course-module-combination [db-con course-id course-module-map module-combination-id module-combination] ; discard module-combinations that have "empty" modules (i.e. modules without actual units)
  (let [modules (map #(Integer/parseInt (:pordnr %)) module-combination)]
    (if (every? identity (map (fn [m] (contains? course-module-map m)) modules))
      (doall (map (fn [m]
             (let [record {:course_id course-id
                           :combination_id module-combination-id
                           :module_id (get course-module-map m)}]
               (insert! db-con :course_modules_combinations record))) modules))
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
        levels (mapv (fn [l] (store-child l db-con nil parent-id modules)) children)]
    (log/debug {:kzfa kzfa :degree degree :course course :name name :po po})
    ; insert course-level/parent-id pairs into course_level table
    (mapv
      (fn [l] (insert! db-con :course_levels {:course_id parent-id :level_id l}))
      levels)
    (store-course-module-combinations db-con c parent-id)))

(defn persist-courses [db-con levels modules]
  (mapv (fn [l] (store-course db-con l modules)) levels))

(defn persist [levels modules units]
  (run-on-db #(store-stuff % levels modules units)))
