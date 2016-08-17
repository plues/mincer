(defproject mincer "2.0.0-SNAPSHOT"
  :description "Tool to create a SQLite database from PlüS module tree and data XML files."
  :url "https://github.com/plues/mincer"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/java.jdbc "0.6.1"]
                 [org.clojure/math.combinatorics "0.1.3"]
                 [org.clojure/test.check "0.9.0"]
                 [org.clojure/tools.cli "0.3.5"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.slf4j/slf4j-log4j12 "1.7.21"]
                 [org.xerial/sqlite-jdbc "3.8.11.2"]
                 [seesaw "1.4.5"]]
  :plugins [[lein-launch4j "0.1.2"]]
  :launch4j-config-file "config.xml"
  :launch4j-install-dir ~(System/getenv "LAUNCH4J_INSTALL_DIR")
  :main mincer.core
  :aot [mincer.core])
