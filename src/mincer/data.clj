(ns mincer.data
  (:gen-class)
  (:require
    [clojure.java.jdbc :as jdbc]
    [clojure.java.jdbc :refer [with-db-connection]]
    [clojure.tools.logging :as log]))

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
                                                     [:min :integer] ; NOTE can be nil if tm and art are set
                                                     [:max :integer] ; NOTE can be nil if tm and art are set
                                                     [:parent_id :integer "REFERENCES levels"]
                                                     [:created_at :datetime :default :current_timestamp]
                                                     [:updated_at :datetime :default :current_timestamp])
                               "CREATE INDEX parent_level ON levels(parent_id)"

                               (jdbc/create-table-ddl :courses
                                                      [:id :integer "PRIMARY KEY" "AUTOINCREMENT"]
                                                      [:degree :string "NOT NULL"]
                                                      [:short_name :string "NOT NULL"]
                                                      [:name :string "NOT NULL"]
                                                      [:kzfa :string "NOT NULL"]
                                                      [:po :string]
                                                      [:created_at :datetime :default :current_timestamp]
                                                      [:updated_at :datetime :default :current_timestamp])

                               (jdbc/create-table-ddl :course_levels
                                                      [:course_id "NOT NULL" "REFERENCES courses"]
                                                      [:level_id "NOT NULL" "REFERENCES levels"])
                               "CREATE INDEX course_level_course ON course_levels(course_id)"
                               "CREATE INDEX course_level_level ON course_levels(level_id)"

                               (jdbc/create-table-ddl :modules
                                                      [:id :integer "PRIMARY KEY" "AUTOINCREMENT"]
                                                      [:level_id :integer "REFERENCES levels"]
                                                      ; XXX consider discarding one of both
                                                      [:name :string "NOT NULL"]
                                                      [:title  :string]
                                                      [:pordnr :integer]
                                                      [:mandatory :boolean]
                                                      ; XXX do we a direct link to the course?
                                                      [:created_at :datetime :default :current_timestamp]
                                                      [:updated_at :datetime :default :current_timestamp])

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
                                                      [:updated_at :datetime :default :current_timestamp]))  
                               "CREATE INDEX session_group_id ON sessions(group_id)"

  (jdbc/insert! db-con :info {:key "schema_version"
                              :value (str "v3.0")})
  (jdbc/insert! db-con :info {:key "generator"
                              :value (str "mincer" "-" mincer-version)}))

(defn insert! [db-con table col]
  (log/debug "Saving to" table col)
  ((keyword "last_insert_rowid()")
   (first (jdbc/insert! db-con table col))))

(defn run-on-db [func]
    (let [database (.getPath (java.io.File/createTempFile "mince" ".sqlite3"))
          db-spec {:classname   "org.sqlite.JDBC"
                   :subprotocol "sqlite"
                   :subname     database}]
      (with-db-connection [db-con db-spec]
        (log/debug database)
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

(defn store-group [db-con unit-id {:keys [type sessions]}]
  (assert (= :group type) type)
  (let [group-id (insert! db-con :groups {:unit_id unit-id})]
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

(defmethod store-child :level [{:keys [:min max name tm art children]} db-con parent-id modules]
  "Insert node into level table in db-con. Returns id of created record."
  (let [record {:parent_id parent-id
                :min min
                :max max
                :tm tm
                :art art
                :name name}
        parent-id (insert! db-con :levels record)]
    (doall (map (fn [l] (store-child l db-con parent-id modules)) children))
    ; return the id of the created record
    parent-id))

(defmethod store-child :module [{:keys [name id pordnr mandatory]} db-con parent-id modules]
  (let [{:keys [title abstract-units course]} (get modules id)
        record {:level_id parent-id
                :mandatory mandatory
                :name name}]
    (log/debug (type title))
    (if-not (nil? title) ; NOTE or use something else
      ; (insert! db-con :modules record)
      ; XXX update course in this case -> need to retreive the course or bubble the data for the udpate
      ; merge both module records
      (let [extended-record (merge record {:pordnr pordnr :title title})
            module-id (insert! db-con :modules extended-record)]
        (mapv (partial store-abstract-unit db-con module-id) abstract-units)
        ; return the id of the created record
        module-id))))

(defmethod store-child :default [child & args]
  (throw  (IllegalArgumentException. (str (:type child)))))

(defn store-course [db-con idx {:keys  [kzfa degree course name po children]} modules]
  (let [params {:degree     degree
                :short_name course
                :kzfa       kzfa ; XXX find a propper name for this
                :name       name
                :po         po}]
    (let [parent-id (insert! db-con :courses params)
          levels (mapv (fn [l] (store-child l db-con nil modules)) children)]
      ; insert course-level/parent-id pairs into course_level table
      (mapv (fn [l] (insert! db-con :course_levels {:course_id parent-id :level_id l})) levels))))

(defn persist-courses [db-con levels modules]
  (reduce-kv (fn [_ k v] (store-course db-con k v modules)) nil levels))

(defn persist [levels modules units]
  (run-on-db #(store-stuff % levels modules units)))
