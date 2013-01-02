(defproject boss "0.1.0-SNAPSHOT"
  :description "Clojure implmentation of Balance Optimization Subset Selection"
  :url ""
  :resources-path "resources"
  :source-paths ["src/clj"]
  :test-paths ["test/clj"]
  :java-source-paths ["src/jvm"]
  :aliases {"cli" ["run" "-m" "boss.cli"]}
  :repositories {"conjars" "http://conjars.org/repo/"}
  :jvm-opts ["-XX:MaxPermSize=128M"
             "-XX:+UseConcMarkSweepGC"
             "-Xms1024M" "-Xmx1048M" "-server"]
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/math.numeric-tower "0.0.1"]
                 [incanter/incanter-core "1.3.0-SNAPSHOT"]
                 [incanter/incanter-charts "1.3.0-SNAPSHOT"]
                 [lein-swank "1.4.4"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [org.clojure/tools.cli "0.2.2"]
                 [cascalog-checkpoint "0.1.1"]
                 [backtype/dfs-datastores "1.1.3"]
                 [backtype/dfs-datastores-cascading "1.2.0"]]
  :profiles {:dev {:dependencies [[org.apache.hadoop/hadoop-core "0.20.2-dev"]
                                  [midje-cascalog "0.4.0"]
                                  [incanter/incanter-charts "1.3.0"]]
                   :plugins [[lein-swank "1.4.4"]
                             [lein-midje "2.0.0-SNAPSHOT"]
                             [lein-emr "0.1.0-SNAPSHOT"]]}}
  )
