(ns boss.cli
  "Command line interface for running BOSS."
  (:gen-class)
  (:use [boss.core]
        [clojure.tools.cli])
  (:require [incanter.core :as i]
            [incanter.charts :as c]))

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

(defn- match
  "Execute the BOSS match command using supplied map of args."
  [args-map]
  (let [N (:num_population args-map)
        n (:num_treatment args-map)
        num-bins (:num_bins args-map)
        data (data-map :N N :n n)
        control-grp (control-group (:control data) (:treatment data) num-bins)]
    (i/view
     (c/histogram (map first control-grp) :nbins 50 :series-label "X1")))
  (println "Match complete."))

(defn -main
  "Entry point for command line interface."
  [& args]
  (let [[args-map trails usage] (parse-args args)
        command (first trails)]
    (if (= command "help")
      (println usage)
      (do
        (println (format "Running BOSS %s: %s" command args-map))
        (cond
         (= command "match") (match args-map)
         :else (println (format "Oops! %s is an unknown command." command)))
        (println "Done!")))))
