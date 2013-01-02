# boss-clj

Clojure implementation of the Balance Optimization Subset Selection method.

## Running locally

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
