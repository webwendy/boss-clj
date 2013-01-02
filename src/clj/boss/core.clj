(ns boss.core
  (:use [incanter.stats :only (sample-normal)]
        clojure.contrib.combinatorics
        [clojure.contrib.math :only (floor)])
  (:require [incanter.core :as i]
            [incanter.stats :as s]
            [incanter.charts :as c]))

;; (defn rand-data [N vcov-mat]
;;   (let [Q (i/decomp-cholesky vcov-mat)
;;         k (second (i/dim Q))
;;         m (take k (repeatedly #(sample-normal N)))]
;;     (i/mmult (i/bind-columns m) Q)))


(defn rand-data
  "Create a data frame with N observations and k cofactors, each
  distributed independently standard normal."
  [N k]
  (take k (repeatedly #(sample-normal N))))

(defn scale-cond
  [thresh x & {:keys [high low] :or {high 2 low 1}}]
  (if (> x thresh) high low))

(defn dgp [xs {:keys [bs] :or {bs [14 7 11 -1]}}]
  (reduce + (map * bs (apply conj [1] xs))))

(defn treat-likelihood [thresholds xs]
  (let [s (map scale-cond thresholds xs)]
    (reduce + (map (comp i/exp *) s xs))))

(defn likelihood-scores [pool]
  (let [sds (map s/sd pool)
        means (map s/mean pool)
        thresh (map + sds means)]
    (map (partial treat-likelihood thresh)
         (apply map vector pool))))

(defn get-idx [idx-coll coll]
  (map #(nth coll %) idx-coll))

(defn sub-sample [pool n]
  (let [ls (likelihood-scores pool)
        sorted-vs (sort-by second > (map-indexed vector ls))
        idx (map first (take (* 2 n) sorted-vs))]
    (take n (shuffle (apply map vector (map (partial get-idx idx) pool))))))

(defn data-map [N k n]
  (let [treatment-pool (rand-data N k)]
    {:control (rand-data N k)
     :treatment-pool treatment-pool
     :treatment-group (sub-sample treatment-pool n)}))

(def data (data-map 1000 3 500))

(defn hist-treatment []
  (let [X (map first (:treatment-group data))]
    (c/histogram X :nbins 50 :series-label "X1")))

(defn bounding-interval [coll]
  [(reduce min coll) (reduce max coll)])

(defn breakpoints [B coll]
  {:post [(= (count %) (inc B))]}
  (let [[t0 tB] (bounding-interval coll)
        step (/ (- tB t0) B)]
    (map #(+ t0 (* step %))
         (range (inc B)))))

(defn moving-intervals
  [bpoint-seq]
  (partition 2 1 bpoint-seq))

(defn all-bins [& [colls]]
  (apply cartesian-product colls))

(defn empirical-bins [pool B]
  (let [breaks (map (partial breakpoints B)
                    (i/trans pool))]
    (all-bins (map moving-intervals breaks))))

(defn in-interval? [[lb ub] x]
  (and (>= x lb) (<= x ub)))

(defn base-count [bin]
  (let [coll (:treatment-group data)]
    (map (partial in-interval? bin) coll)))

(defn general-interval [bound-seq val-seq]
  (every? true? (map in-interval? bound-seq val-seq)))

(defn val->bin [val bins]
  (let [truth-seq (map #(general-interval % val) bins)]
    (ffirst (filter #(true? (second %))
                    (map vector bins truth-seq)))))

(defn interval-count [bins pool]
  (frequencies (map #(val->bin % bins) pool)))

;; (defn ref-freq [pool B]
;;   (let [bins (empirical-bins pool B)]
;;     (map interval-count bins pool)))

;; (defn in-interval?
;;   ([[lb ub] x]
;;      (and (>= x lb) (<= x ub)))
;;   ([[[lb0 ub0] [lb1 ub1]] x0 x1]
;;      (and (in-interval? [lb0 ub0] x0)
;;           (in-interval? [lb1 ub1] x1))))

(defn bin-count [coll1 coll2 bin]
  (let [truth-seq (map (partial in-interval? bin) coll1 coll2)]
    (count (filter true? truth-seq))))

(defn count-seq [coll1 coll2 bins]
  (map (partial bin-count coll1 coll2) bins))

(defn obj-fn [cb tb]
  (let [d (- cb tb)]
    (/ (* d d)
       (reduce max [1 tb]))))

