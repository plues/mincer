(ns mincer.core
  (:gen-class)
  (:require
    [clojure.string :as string]
    [clojure.tools.cli :refer [parse-opts]]
    [clojure.java.io :refer [as-file copy]]
    [mincer.xml.modules :as modules]
    [mincer.xml.tree :as tree]
    [mincer.data :refer [persist]]))


(def cli-options
  ;; An option with a required argument
  [["-t" "--module-tree FILE" "XML-File containing the course and module tree"
    :parse-fn as-file
    :validate [#(.exists (as-file %)) "File not found"]]
  ;; An option with a required argument
   ["-d" "--module-data FILE" "XML-File containing the course and module data"
    :parse-fn as-file
    :validate [#(.exists (as-file %)) "File not found"]]
   ["-o" "--output FILE" "Target file"
    :default "data.sqlite3" ; add date to filename ? ; use custom extension?
    :parse-fn as-file
    ]
   ["-h" "--help"]])

(defn usage [options-summary]
  (->> ["Usage: mincer [options]"
        ""
        "Options:"
        options-summary]
       (string/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (string/join \newline errors)))

(defn start-cli [module-tree module-data target]
  (let [data (modules/process module-data)
        tree (tree/process module-tree)
        db (persist tree (:modules data) (:units data))]
    (copy (as-file db) (as-file target))))

(defn start-gui [] (println "gui"))

(defn -main
  [& args]
  ;; work around dangerous default behaviour in Clojure
  (alter-var-root #'*read-eval* (constantly false))
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    ;; Handle help and error conditions
    (cond
      (:help options) (usage summary)
      errors (error-msg errors)
      (= (count options) 3) (start-cli (:module-tree options) (:module-data options) (:output options))
      (= (count options) 0) (start-gui)
      (true) (usage summary))))


(defn process-data []
  (start-cli "test-data/Modulbaum.xml" "test-data/Moduldaten-full.xml" "mincer.sqlite3"))
