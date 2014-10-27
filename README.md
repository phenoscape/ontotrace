# ontotrace

Ontotrace is a tool for querying a local copy of the Phenoscape Knowledgebase and generating an inferred presence/absence data matrix, for a given taxon and anatomy specification.

## Run
To run Ontotrace you must have a working installation of Java (7 or higher). You can download a prebuilt Ontotrace package from the [GitHub Releases page](https://github.com/phenoscape/ontotrace/releases). Start Ontotrace by running `./ontotrace` within the `bin` folder. You will need to provide seven arguments in this order:

* `--informative`|`--all`—output only variable characters or all characters with either presence or absence data
* `bigdata.properties`—path to Bigdata properties file
* `bigdata.jnl`—path to Phenoscape KB Bigdata journal database file
* `tbox.owl`—path to Phenoscape KB tbox OWL file
* `anatomy.dl`—path to text file containing input anatomy expression using OWL Manchester syntax
* `taxonomy.dl`—path to text file containing input taxonomy expression using OWL Manchester syntax
* `result.xml`—path for output file

## Build
Ontotrace is written in Scala. To build it you must first have SBT installed. If you are using Homebrew on Mac OS X, simply run `brew install sbt`. Otherwise, follow the instructions on the [SBT website](http://www.scala-sbt.org).

To build a packaged version containing a runnable script and all dependencies, run `sbt stage`. This will create a relocatable folder at `target/universal/stage` which has executable scripts within `bin` and library dependencies within `lib`.

To create an archive for release, run `sbt universal:packageZipTarball`. The archive will be created within `target/universal`.