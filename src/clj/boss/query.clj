(ns boss.query
  (:use boss.core
        cascalog.api)
  (:require [incanter.core :as i]
            [incanter.stats :as s]
            [incanter.charts :as c]
            [cascalog.ops :as ops]))

(defn tester []
  (let [data (data-map 100 5)
        src  (:control data)]
    (?<- (stdout)
         [?x1 ?x2 ?x3]
         (src ?x1 ?x2 ?x3))))


