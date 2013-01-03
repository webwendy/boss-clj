# boss-clj

Clojure implementation of the Balance Optimization Subset Selection method.

## Installation

If you have Git, Clojure, and Lein already installed, you're all set:

```bash
$ ./boss help
Usage:

 Switches              Default            Desc                                                    
 --------              -------            ----                                                    
 -c, --num_covariants  3                  Number of covariants.                                   
 -t, --num_treatment   500                Number of people in treatment group.                    
 -b, --num_bins        4                  Number of uniformly sized data bins for each covariant. 
 -p, --num_population  100000             Number of people in population group.                   
 -w, --workspace       /tmp/hylo-sim.png  Workspace directory to save graphs to. 
```

Otherwise, to easily download everything and run it on your Mac, follow these steps:

1. Download and install [Git](https://central.github.com/mac/latest).
1. Download and save the [setup](https://github.com/eightysteele/boss-clj/blob/master/setup) script to your Desktop.
1. Open your Terminal app.

Next, make the script executable and run it:

```bash
cd ~/Desktop
chmod a+x setup
./setup
```

That will take a little while to download and install everything. When it is done, you're all set:

```bash
cd boss-clj
./boss help
```

## Command Line Interface

Right now you can run the `match` command which matches the control group with 
the covariant distribution of the treatment group. For example, here's matching 
a treatment group of 500 against a control group of 500000, using 8 bins, and
writing the resulting histogram graph to `/tmp/boss-match.png`:

```bash
$ ./boss match -t 500 -p 500000 -b 8 -w /tmp/boss-match.png
```

## Initial results

We start with a control and treatment pool, each with 100,000
observations.  From the treatment pool, we select a subsample with
certain characteristics to represent the treatment group -- those who
actually received treatment.  This distribution of the characteristics
of the treatment group cannot easily be parameterized.  The idea is to
non-parametrically select a group from the control pool that closely
resembles the treatment group.

Consider, for example, the data generating process in Equation (6) of
the Cho (2012) paper on the BOSS algorithm.  Each observation has
three characteristics, such that the covariate matrix is of dimension
_N x 3_.  Each covariate is distributed standard normal.  The
distribution of the first covariate is diplayed below for both the
control- and treatment-pool:

![](https://dl.dropbox.com/u/5365589/control-pool.png)

The distribution for the same covariate in among the treated
observations (n = 500) is much stranger, bimodal and impossible to
characterize with a simple function.

![](https://dl.dropbox.com/u/5365589/treated.png)

We can, however, select similar observations from the control pool.
The BOSS algorithm is well-suited for a Clojure/Cascalog
implementation, since the tasks for each bin can be sent to a separate
mapper on a Hadoop cluster.  That is, the algorithm is highly
parallelizable.  The implementation is reasonably simple to call from
higher-order functions in the `boss.core` namespace:

```clojure
(def data (data-map :N 100000 :n 500))

(let [control-grp (control-group (:control data) (:treatment data) 16)]
  (i/view
   (c/histogram (map first control-group) :nbins 50 :series-label "X1")))
```

The resulting histogram looks much more similar to the covariate
distribution of the treatment group:

![](https://dl.dropbox.com/u/5365589/control-group.png)

And indeed, the estimated impact is much closer to the true impact.
Consider one run of the algorithm with B = 16.  Without implementing
the BOSS algorithm, the estimated outcome of the control pool is 13.94
with a standard deviation of 13.08.  The estimated outcome of the
treated group is 40.89 with a std. dev. of 11.03.  The results suggest
that there _was_ a treatment effect, when we know that there is not.
We generated the data, and know for a fact that the treatment effect
should be zero.  The estimated outcome of the BOSS control group,
however, is 39.65 with a std. dev. of 11.37.  There is no longer a
treatment effect, reflecting the true data generating process.

This process is very similar to propensity score matching, except that
the data is not collapsed to a single dimension before the matching.
Rather, the matching occurs on the raw covariates.  This requires much
more computational power -- which we have.  It also requires a lot
more data, since the binning triggers the curse of dimensionality.  We
have big data.

This is only a very rough draft.  There are some minor issues in the
processing.  But it is clear that the algorithm works as intended.


