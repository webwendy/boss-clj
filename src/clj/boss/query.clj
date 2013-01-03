(ns boss.query
  (:use boss.core
        cascalog.api)
  (:require [incanter.core :as i]
            [incanter.stats :as s]
            [incanter.charts :as c]
            [cascalog.ops :as ops]))

(defn mean-effect [src]
  (<- [?ya]
      (src ?x1 ?x2 ?x3)
      (dgp ?x1 ?x2 ?x3 :> ?y)
      (ops/avg ?y :> ?ya)))

(defn impact-diff [src-c src-t]
  (let [extract-mean (fn [src] (ffirst (??- (mean-effect src))))]
    (i/abs (apply map - (map extract-mean [src-c src-t])))))

(defn tester []
  (let [data (data-map 100000 500)
        treatment-src (:treatment data)
        control-src (:control data)]
    (impact-diff control-src treatment-src)))
