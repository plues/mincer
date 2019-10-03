(defproject mincer "3.4.0-SNAPSHOT"
  :description "Tool to create a SQLite database from PlüS module tree and data XML files."
  :url "https://github.com/plues/mincer"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/java.jdbc "0.7.10"]
                 [org.clojure/math.combinatorics "0.1.6"]
                 [org.clojure/test.check "0.10.0"]
                 [org.clojure/tools.cli "0.4.2"]
                 [org.clojure/tools.logging "0.5.0"]
                 [org.slf4j/slf4j-log4j12 "1.7.28"]
                 [org.xerial/sqlite-jdbc "3.28.0"]
                 [seesaw "1.5.0"]]
  :plugins [[jonase/eastwood "0.2.3"]
            [lein-kibit "0.1.2"]
            [lein-launch4j "0.1.2"]]
  :launch4j-config-file "config.xml"
  :launch4j-install-dir ~(System/getenv "LAUNCH4J_INSTALL_DIR")
  :profiles {:uberjar {:aot :all}}
  :main mincer.core)
