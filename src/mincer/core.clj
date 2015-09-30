(ns mincer.core
  (:gen-class)
  (:require
    [clojure.string :as string]
    [clojure.tools.cli :refer [parse-opts]]
    [clojure.java.io :refer [as-file]]
    [mincer.xml.modules :as modules]
    [mincer.xml.tree :as tree]))


(def cli-options
  ;; An option with a required argument
  [["-t" "--module-tree FILE" "XML-File containing the course and module tree"
    :parse-fn as-file
    :validate [#(.exists (as-file %)) "File not found"]]
  ;; An option with a required argument
   ["-d" "--module-data FILE" "XML-File containing the course and module data"
    :parse-fn as-file
    :validate [#(.exists (as-file %)) "File not found"]]
   ;; A boolean option defaulting to nil
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

(defn start-cli [module-tree module-data]
  (let [data (modules/process module-data)
        tree (tree/process module-tree)]))
  ; TODO merge tree
  ; (let [tree (get-xml module-tree)
  ;       module-info (get-xml module-data)]
  ;   (tree-to-module-map module-info)))

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
      (= (count options) 2) (start-cli (:module-tree options) (:module-data options))
      (= (count options) 0) (start-gui)
      (true) (usage summary))))
