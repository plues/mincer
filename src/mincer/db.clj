(ns mincer.db
  (:gen-class)
  (:require
    [clojure.java.jdbc :as jdbc]
    [clojure.java.jdbc :refer [with-db-transaction]]
    [clojure.tools.logging :as log]))

(def mincer-version "3.3.0-SNAPSHOT") ; updated with bumpversion

(defn now [] (new java.util.Date))

(defn setup-db [db-con]
  ; maybe use DDL for schema
  (jdbc/db-do-commands  db-con [(jdbc/create-table-ddl :info
                                                      [[:key :string "NOT NULL"]
                                                       [:value :string "DEFAULT ''" "NOT NULL"]])
                               "CREATE UNIQUE INDEX info_key ON info(key)"
                               "PRAGMA foreign_keys = ON"
                               (jdbc/create-table-ddl :levels
                                                     [[:id :integer "PRIMARY KEY" "AUTOINCREMENT"]
                                                      [:name :string "NOT NULL"]
                                                      [:tm :string]
                                                      [:art :string]
                                                      [:min :integer] ; NOTE can be nil if tm and art are set or if we are using credit points
                                                      [:max :integer] ; NOTE can be nil if tm and art are set or if we are using credit points
                                                      [:min_credit_points :integer :default "NULL"] ; NOTE can be nil
                                                      [:max_credit_points :integer :default "NULL"] ; NOTE can be nil
                                                      [:parent_id :integer "REFERENCES levels"]
                                                      [:created_at :datetime :default :current_timestamp]
                                                      [:updated_at :datetime :default :current_timestamp]])
                               "CREATE INDEX parent_level ON levels(parent_id)"

                               (jdbc/create-table-ddl :courses
                                                      [[:id :integer "PRIMARY KEY" "AUTOINCREMENT"]
                                                       [:key :string "NOT NULL UNIQUE"]
                                                       [:degree :string "NOT NULL"]
                                                       [:short_name :string "NOT NULL"]
                                                       [:name :string "NOT NULL"]
                                                       [:kzfa :string "NOT NULL"]
                                                       [:po :integer]
                                                       [:credit_points :integer :default "NULL"]
                                                       [:created_at :datetime :default :current_timestamp]
                                                       [:updated_at :datetime :default :current_timestamp]])
                               "CREATE INDEX course_key ON courses(key)"

                               (jdbc/create-table-ddl :course_levels
                                                      [[:course_id "NOT NULL" "REFERENCES courses"]
                                                       [:level_id "NOT NULL" "REFERENCES levels"]])
                               "CREATE INDEX course_level_course ON course_levels(course_id)"
                               "CREATE INDEX course_level_level ON course_levels(level_id)"


                               (jdbc/create-table-ddl :modules
                                                      [[:id :integer "PRIMARY KEY" "AUTOINCREMENT"]
                                                       [:key :string "NOT NULL"]
                                                       ; XXX consider discarding one of both
                                                       [:title  :string]          ; from module data
                                                       [:pordnr :integer "UNIQUE"]
                                                       [:elective_units :integer]
                                                       [:bundled :boolean]
                                                       [:created_at :datetime :default :current_timestamp]
                                                       [:updated_at :datetime :default :current_timestamp]])

                              (jdbc/create-table-ddl :module_levels
                                                      [[:id :integer "PRIMARY KEY" "AUTOINCREMENT"]
                                                       [:module_id "NOT NULL" "REFERENCES modules"]
                                                       [:level_id "NOT NULL" "REFERENCES levels"]
                                                       [:course_id "NOT NULL" "REFERENCES courses"] ; link to root
                                                       [:name :string "NOT NULL"] ; from module tree
                                                       [:mandatory :boolean]
                                                       [:credit_points :integer :default "NULL"]
                                                       [:created_at :datetime :default :current_timestamp]
                                                       [:updated_at :datetime :default :current_timestamp]])

                              "CREATE INDEX module_levels_module ON module_levels(module_id)"
                              "CREATE INDEX module_levels_level ON module_levels(level_id)"

                               (jdbc/create-table-ddl :course_modules_combinations
                                                      [[:id :integer "PRIMARY KEY" "AUTOINCREMENT"]
                                                       [:course_id "REFERENCES courses"]
                                                       [:combination "BLOB"]])
                               "CREATE INDEX course_modules_combinations_course ON course_modules_combinations(course_id)"

                               (jdbc/create-table-ddl :abstract_units
                                                      [[:id :integer "PRIMARY KEY" "AUTOINCREMENT"]
                                                       [:key :string "NOT NULL UNIQUE"]
                                                       [:title :string "NOT NULL"]
                                                       [:created_at :datetime :default :current_timestamp]
                                                       [:updated_at :datetime :default :current_timestamp]])

                               (jdbc/create-table-ddl :modules_abstract_units_semesters
                                                      [[:abstract_unit_id :int "NOT NULL" "REFERENCES abstract_units"]
                                                       [:module_id :int "NOT NULL" "REFERENCES modules"]
                                                       [:semester :int "NOT NULL"]])
                               "CREATE INDEX modules_abstract_units_semesters_au_id ON modules_abstract_units_semesters(abstract_unit_id)"
                               "CREATE INDEX modules_abstract_units_semesters_module_id ON modules_abstract_units_semesters(module_id)"

                               (jdbc/create-table-ddl :modules_abstract_units_types
                                                      [[:abstract_unit_id :int "NOT NULL" "REFERENCES abstract_units"]
                                                       [:module_id :int "NOT NULL" "REFERENCES modules"]
                                                       [:type :string "NOT NULL"]])
                               "CREATE INDEX modules_abstract_units_types_au_id ON modules_abstract_units_types(abstract_unit_id)"
                               "CREATE INDEX modules_abstract_units_types_module_id ON modules_abstract_units_types(module_id)"


                               (jdbc/create-table-ddl :unit_abstract_unit
                                                      [[:unit_id :int "NOT NULL" "REFERENCES unit"]
                                                       [:abstract_unit_id :int "NOT NULL" "REFERENCES abstract_unit"]])
                               "CREATE INDEX unit_abstract_unit_unit_id ON unit_abstract_unit(unit_id)"
                               "CREATE INDEX unit_abstract_unit_abstract_unit_id ON unit_abstract_unit(abstract_unit_id)"

                               (jdbc/create-table-ddl :unit_semester
                                                      [[:unit_id :int "NOT NULL" "REFERENCES unit"]
                                                       [:semester :int "NOT NULL"]])
                               "CREATE INDEX unit_semester_unit_id ON unit_semester(unit_id)"

                               (jdbc/create-table-ddl :units
                                                      [[:id :integer "PRIMARY KEY" "AUTOINCREMENT"]
                                                       [:unit_key :string "NOT NULL"]
                                                       [:title :string "NOT NULL"]
                                                       [:created_at :datetime :default :current_timestamp]
                                                       [:updated_at :datetime :default :current_timestamp]])
                               "CREATE INDEX unit_key ON units(unit_key)"

                                (jdbc/create-table-ddl :groups
                                                      [[:id :integer "PRIMARY KEY" "AUTOINCREMENT"]
                                                       [:unit_id :int "NOT NULL" "REFERENCES units"]
                                                       [:half_semester :integer "NOT NULL"]
                                                       [:created_at :datetime :default :current_timestamp]
                                                       [:updated_at :datetime :default :current_timestamp]])
                               "CREATE INDEX group_unit_id ON groups(unit_id)"

                                 (jdbc/create-table-ddl :sessions
                                                      [[:id :integer "PRIMARY KEY" "AUTOINCREMENT"]
                                                       [:group_id :integer "NOT NULL" "REFERENCES groups"]
                                                       [:day :string "NOT NULL"]
                                                       [:time :integer "NOT NULL"]
                                                       [:duration :integer "NOT NULL"]
                                                       [:rhythm :integer "NOT NULL"]
                                                       [:tentative :integer "NOT NULL DEFAULT 0"]
                                                       [:created_at :datetime :default :current_timestamp]
                                                       [:updated_at :datetime :default :current_timestamp]])
                               "CREATE INDEX session_group_id ON sessions(group_id)"

                                 (jdbc/create-table-ddl :minors
                                                      [[:id :integer "PRIMARY KEY" "AUTOINCREMENT"]
                                                       [:course_id :integer "NOT NULL" "REFERENCES courses"]
                                                       [:minor_course_id :integer "NOT NULL" "REFERENCES courses"]
                                                       [:created_at :datetime :default :current_timestamp]
                                                       [:updated_at :datetime :default :current_timestamp]])
                               "CREATE INDEX minor_course_id ON minors(course_id)"

                                (jdbc/create-table-ddl :log
                                                      [[:id :integer "PRIMARY KEY" "AUTOINCREMENT"]
                                                       [:session_id :integer "NOT NULL" "REFERENCES sessions"]
                                                       [:srcDay :string]
                                                       [:srcTime :integer]
                                                       [:targetDay :string]
                                                       [:targetTime :integer]
                                                       [:created_at :datetime :default :current_timestamp]])
                               "CREATE INDEX log_session_id ON log(session_id)"])

  (jdbc/insert! db-con :info {:key "schema_version"
                              :value (str "9")}) ; NOTE: make sure this stays an integer
  (jdbc/insert! db-con :info {:key "generator"
                              :value (str "mincer" "-" mincer-version)})
  (jdbc/insert! db-con :info {:key "generated"
                              :value (str (now))}))

(defn query-course [db-con dict kzfa]
  (log/debug "Query course from database table 'courses' using the dictionary " dict " and kzfa " kzfa)
  (let [db-entry (first (jdbc/query db-con ["SELECT id FROM courses WHERE kzfa=? AND short_name=? AND po=?" kzfa (dict :short_name) (dict :po)]))]
    (if (nil? db-entry) nil (db-entry :id))))

(defn insert! [db-con table rec]
  (log/debug "Saving to " table rec)
  ((keyword "last_insert_rowid()")
   (first (jdbc/insert! db-con table rec))))

(defn insert-all! [db-con table recs]
  (log/debug "Saving to" table recs)
  (jdbc/insert-multi! db-con table recs))

(defn run-on-db [func]
    (let [database (.getPath (java.io.File/createTempFile "mincer" ".sqlite3"))
          db-spec {:classname   "org.sqlite.JDBC"
                   :subprotocol "sqlite"
                   :subname     database}]
      (log/debug "Database " database)
      (with-db-transaction [db-con db-spec]
        (setup-db db-con)
        (func db-con))
      ; return path to generated DB
      database))

(defn abstract-unit-by-key [db-con key]
  (let [records (jdbc/query db-con ["SELECT * FROM abstract_units WHERE key = ?" key])]
    (when (= 1 (count records))
      (first records))))

(defn module-by-pordnr [db-con pordnr]
  (let [records (jdbc/query db-con ["SELECT * FROM modules WHERE pordnr = ?" pordnr])]
    (when (= 1 (count records))
      (first records))))

(defn load-course-module-map [db-con course-id]
  (let [sql "SELECT DISTINCT modules.pordnr, modules.id FROM module_levels JOIN modules ON module_levels.module_id WHERE course_id = ?;"
        res (jdbc/query db-con [sql course-id])]
    (into {} (map (fn [{:keys [pordnr id]}] [pordnr id]) res))))
