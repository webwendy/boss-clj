(ns boss.query
  (:use boss.core
        cascalog.api)
  (:require [incanter.core :as i]
            [incanter.stats :as s]
            [incanter.charts :as c]
            [cascalog.ops :as ops]))

(def sub-pool (range 5))
(def pool     (range 100))

(defn my-func [coll]
  (prn coll)
  [(reduce + coll)])


(defn replace-chunk [[sub-vec]]
  (vec (conj (take 4 (shuffle sub-vec))
             (nth pool (rand-int 99)))))

(defmapcatop exploder [N sub-vec]
  (let [f (fn [_]  (set (replace-chunk sub-vec)))]
    (prn (vec
          (map vec
               (set (map f (range N))))))
    [[(map vec
           (set (map f (range N))))]]))

(defn boss-query
  [p-results]
  (let [src [[[p-results]]]]
    (??<- [?f-val]
          (src ?a)
          (exploder 10 ?a :> ?b)
          (my-func ?b :> ?f-val))))

   ;; (??<- [?trial-vec]
   ;;        (src ?orig-vec)
   ;;        (explode-vec 10 ?orig-vec :> ?trial-vec)
   ;;        (my-func ?trial-vec :> ?f-val)
   ;;        (ops/max ?f-val))




(def idx-group (set (range 20)))
(def idx-pool  (set (range 100)))


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


