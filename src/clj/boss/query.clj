(ns boss.query
  (:use boss.core
        cascalog.api)
  (:require [incanter.core :as i]
            [incanter.stats :as s]
            [incanter.charts :as c]
            [cascalog.ops :as ops]))

(def sub-pool (range 5))
(def pool     (range 100))

(defn my-func [[coll]]
  [(reduce + coll)])

(defn replace-chunk [sub-vec]
  (vec (conj (take 4 (shuffle sub-vec))
             (nth pool (rand-int 99)))))

(defmapcatop boom
  [N tuple]
  (let [tuples (for [x (range N)] (vec (set (replace-chunk tuple))))
        tuples (map vec (set tuples))]
    [[tuples]]))

(defn bossmode
  "> (pprint (bossmode [[[1 2 3 4]]]))
   ([([0 1 2 3 4]
       [1 2 3 4 70]
       [1 2 3 4 40]
       [1 2 3 4 19]
       [1 2 3 4 51]
       [1 2 3 4 86]
       [1 2 3 4 23]
       [1 2 3 4 26]
       [1 2 3 4 91]
       [1 2 3 4 95])
      10])"
  [src]
  (??<- [?b ?f-val]
        (src ?a)
        (boom 10 ?a :> ?b)
        (my-func ?b :> ?f-val)
        (ops/max ?f-val :> ?f-val)))

(def idx-group (vec (set (range 20))))
(def idx-pool  (vec (set (range 100))))

(defstruct obs :id :attr)

(defn random-subgroup [idx-coll n]
  (take n (shuffle idx-coll)))

(defn grab-new [orig-coll new-coll n]
  {:pre [(< n (count orig-coll))]}
  (let [num-orig (- (count orig-coll) n)]
    (concat (take num-orig orig-coll)
            (take n ))))

(defn mean-effect [src]
  (<- [?ya]
      (src ?x1 ?x2 ?x3)
      (dgp ?x1 ?x2 ?x3 :> ?y)
      (ops/avg ?y :> ?ya)))

;; (defn distribute-proc []
;;   (let [data (data-map 100000 500)
;;         treatment-src (:treatment data)]
;;     (reference-freq ?)))

(defn impact-diff [src-c src-t]
  (let [extract-mean (fn [src] (ffirst (??- (mean-effect src))))]
    (i/abs (apply map - (map extract-mean [src-c src-t])))))

(defn tester []
  (let [data (data-map 100000 500)
        treatment-src (:treatment data)
        control-src (:control data)]
    (impact-diff control-src treatment-src)))


;; (defn boss-impact [B]
;;   (let [data (data-map 100000 500)
;;         treatment-src (:treatment data)
;;         control-src (control-group (:control data) treatment-src B)]
;;     (impact-diff control-src treatment-src)))


