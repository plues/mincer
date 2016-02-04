(defproject mincer "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/java.jdbc "0.4.2"]
                 [org.clojure/math.combinatorics "0.1.1"]
                 [org.xerial/sqlite-jdbc "3.8.11.2"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/tools.cli "0.3.3"]
                 [seesaw "1.4.5"]
                 [org.slf4j/slf4j-log4j12 "1.7.14"]
                 [org.clojure/test.check "0.9.0"]]
  :main mincer.core)
