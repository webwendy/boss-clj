(ns boss.core
  (:use [incanter.stats :only (sample-normal)]
        [clojure.contrib.math :only (floor)])
  (:require [incanter.core :as i]
            [incanter.stats :as s]
            [incanter.charts :as c]))

(defn rand-data
  "generates an (N x k) data set that is distributed joint normal,
  according to the supplied variance-covariance incanter matrix;
  default is an (N x 3) independently normal data set."
  [N & {:keys [vcov-mat] :or {vcov-mat (i/identity-matrix 3)}}]
  (let [Q (i/decomp-cholesky vcov-mat)
        k (second (i/dim Q))
        m (take k (repeatedly #(sample-normal N)))]
    (i/to-vect
     (i/mmult (i/trans Q) (i/bind-columns m)))))

(defn scale-cond
  "accepts a threshold and x. if x exceeds the threshold, then returns
  the value scaled by the high factor; otherwise returns the value
  scaled by the low factor."
  [thresh x & {:keys [high low] :or {high 1.25 low 1}}]
  (if (> x thresh)
    (* x high)
    (* x low)))

(defn dgp
  "accepts a vector of characteristics and a vector of parameter
  coefficients.  returns the non-stochastic value from the data
  generating process.  the default reflects the DGP with three
  characteristics found in Equation (6) on page 8 of the Cho (2012)
  paper."
  [xs {:keys [bs] :or {bs [14 7 11 -1]}}]
  (reduce + (map * bs (apply conj [1] xs))))

(defn treat-likelihood
  "accepts the thresholds for the supplied collection, returns the
  likelihood of being treated"
  [thresholds coll]
  (let [scale-coll (map scale-cond thresholds coll)]
    (reduce + (map i/exp scale-coll))))

(defn likelihood-scores
  "accepts the treatment pool and returns the likelihoods of being
  treated, based on the individual characteristics and the functional
  form in `treat-likelihood`"
  [pool]
  (let [[sds means] (map (juxt s/sd s/mean) pool)
        thresh (map + sds means)]
    (map (partial treat-likelihood thresh)
         (apply map vector pool))))

(defn get-idx
  "accepts a collection and a vector of indices, returns the elements
  from the collection at the supplied indices; mostly just a
  convenient wrapper to map `nth` over multiple indices."
  [idx-coll coll]
  (map #(nth coll %) idx-coll))

(defn sub-sample
  "accepts the treatment pool and returns a sub sample of `n`
  individuals, after some arbitrary handwaving TODO: CLEAN THIS UP.
  ugly."
  [pool n]
  (let [ls (likelihood-scores pool)
        sorted-vs (sort-by second > (map-indexed vector ls))
        idx (map first (take (* 2 n) sorted-vs))]
    (take n (shuffle (apply map vector (map (partial get-idx idx) pool))))))

(defn data-map
  "accepts the number of observations in each pool and the number in
  the treatment group `n`. returns a convenient data map for the
  relevant (sub)samples."
  [N n]
  (let [treatment-pool (rand-data N)]
    {:control (rand-data N)
     :treatment-group (sub-sample treatment-pool n)}))

(defn hist-treatment
  "plot the histogram for the treatment group."
  [& {:keys [idx-fn] :or {idx-fn first}}]
  (let [data (data-map 100000 500)
        X (map idx-fn (:treatment-group data))]
    (c/histogram X :nbins 50 :series-label "X1")))

(defn bounding-interval
  "accepts a collection of numbers and returns the bounding interval,
  upper- and lower-bound, as a tuple."
  [coll]
  [(reduce min coll) (reduce max coll)])

(defn breakpoints
  "accepts a collection and the number of uniform bins to split the
  collection into.  returns a sequence of B+1 breakpoints inclusive of
  the end points of the collection"
  [B coll]
  {:post [(= (count %) (inc B))]}
  (let [[t0 tB] (bounding-interval coll)
        step (/ (- tB t0) B)]
    (map #(+ t0 (* step %))
         (range (inc B)))))

(defn moving-intervals
  "accepts a sequence of breakpoints and returns a series of B
  intervals, where B is the number of bins."
  [bpoint-seq]
  {:post [= (count %) (dec (count bpoint-seq))]}
  (partition 2 1 bpoint-seq))

(defn all-bins
  "accepts a sequence of interval sequences for multiple collections
  and returns the cartesian product (of arbitrary dimension) for the
  k=3 supplied collections. appropriately destructures the arguments."
  [& [colls]]
  (map vec (apply cartesian-product colls)))

(defn treatment-bins
  "accepts the treatment pool represented by a (k x N) vector of
  vectors (not incanter matrix) and returns all bins for the joint
  distribution of the observed treatment group."
  [pool B]
  (let [breaks (map (partial breakpoints B)
                    (apply map vector pool))]
    (all-bins (map moving-intervals breaks))))

(defn in-interval?
  "accepts a bounding interval and a value. returns a boolean, true if
  the value falls in the interval with some padding on either side to
  account for rounding error."
  [[lb ub] x]
  (and (>= x (- lb 0.00001))
       (<= x (+ ub 0.00001))))

(defn general-interval
  "accepts a sequence of bounding intervals and a sequence of
  values. returns true only if every value falls into its
  corresponding interval."
  [bound-seq val-seq]
  (every? true? (map in-interval? bound-seq val-seq)))

(defn val->bin
  "accepts a value and a set of bins.  returns the bin that the value
  falls into."
  [val bins]
  (let [truth-seq (map #(general-interval % val) bins)]
    (ffirst (filter #(true? (second %))
                    (map vector bins truth-seq)))))

(defn ref-freq
  "accepts the full treatment pool (k x N) and a set of bins, and
  returns the vectorized version of a map with the values being
  frequencies.  the return structure is necessary for cascalog, which
  does not destructure hash-maps"
  [treatment-pool bins]
  (map vec
       (frequencies (map #(val->bin % bins)
                         treatment-pool))))

(defn find-obs
  "accepts the N observation control pool along with a particular bin
  and the number of observations that are in that bin in the treatment
  group (supplied as an element of the vectorized hash-map from
  `ref-freq`). returns a random sampling of the observations if there
  are more candidates than needed."
  [control-pool [bin count]]
  (take count
        (shuffle (filter (partial general-interval bin)
                         (apply map vector control-pool)))))

(defn control-group
  "accepts the control pool, the treatment group, and the number of
  bins. returns the observations in the new, matched control group.
  hardcoded to work only with k=3."
  [control-pool treatment-group B]
  (let [bins (treatment-bins treatment-group B)
        ref (ref-freq treatment-group bins)]
    (partition 3 (flatten
                  (map (partial find-obs control-pool) ref)))))

(defn obj-fn
  "accepts the number of hits in a particular bin within the control
  `cb` and `treatment` group, and returns the normalized, squared
  difference. component function of the objective function in
  Equation (5) on page 7 of Cho (2012)."
  [cb tb]
  (let [d (- cb tb)]
    (/ (* d d)
       (reduce max [1 tb]))))
