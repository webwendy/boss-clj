(ns boss.cli
  "Command line interface for running BOSS."
  (:gen-class)
  (:use [boss.core]
        [clojure.tools.cli]))

(defn parse-args
  "Parse command line argument string."
  [args]
  (try
    (cli args
         ["-c" "--num_covariants" "Number of covariants."
          :parse-fn #(Integer. %)
          :default 3]
         ["-t" "--num_treatment" "Number of people in treatment group."
          :parse-fn #(Integer. %)
          :default 500]
         ["-b" "--num_bins" "Number of uniformly sized data bins for each covariant."
          :parse-fn #(Integer. %)
          :default 4]
         ["-p" "--num_population" "Number of people in population group."
          :parse-fn #(Integer. %)
          :default 100000]
         ["-w" "--workspace" "Workspace directory to save graphs to."
          :default "/tmp/hylo-sim.png"])
    (catch Exception e (do (println (.getMessage e)) nil))))

(defn -main
  "Entry point for command line interface."
  [& args]
  (when-let [[args-map trails usage] (parse-args args)]
    (if (= "help" (first trails))
      (println usage)
      (do
        (println "Running BOSS:" args-map)
        ;; TODO
        (println "Done!")))))
