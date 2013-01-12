(ns boss.query
  (:use boss.core
        cascalog.api)
  (:require [incanter.core :as i]
            [incanter.stats :as s]
            [incanter.charts :as c]
            [cascalog.ops :as ops]))

(def sub-pool (range 5))
(def pool     (range 100))

(defn my-func
  [coll]
  (reduce + coll))

(defn all-distinct?
  [coll]
  (= (count coll) (count (distinct coll))))

(defn replace-chunk
  [coll new-chunk]
  (let [n (- (count coll) (count new-chunk))
        x (flatten (cons new-chunk
                         (take n (shuffle coll))))]
    (if (all-distinct? x) x nil)))

(defn random-subgroup
  [coll n]
  (take n (shuffle coll)))

(defn sub-cols [N n coll]
  (let [f (fn [_] (set (random-subgroup coll n)))]
    (map vec (distinct
              (map f (range N))))))

(defmapcatop explode-group [coll]
  (let [chunks (sub-cols 100 2 pool)]
    (vec
     (for [c chunks]
       [(vec (replace-chunk coll c))]))))
 
(defn iter-step [prev-coll]
  (??<- [?b ?c]
        ([[prev-coll]] :> ?a)
        (explode-group ?a :> ?b)
        (my-func ?b :> ?c)))

(defn process-res [prev-coll]
  (ffirst (sort-by second > (iter-step prev-coll))))

(defn test-loop [orig-coll]
  (loop [coll orig-coll
         i 10]
    (if (neg? i)
      coll
      (recur (process-res coll)
             (dec i)))))
