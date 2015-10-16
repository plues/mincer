# mincer

Tool to create a SQLite database from module tree and data files.

## Installation

Download a [nightly build](http://nightly.cobra.cs.uni-duesseldorf.de/slottool/mincer/)

## Usage

Mincer supports two usage modes: batch/CLI and GUI modes.

### GUI

To start the graphical user-interface run the provided jar without arguments:

```
$ java -jar mincer-0.1.0-SNAPSHOT-standalone.jar
```

### CLI

The batch mode requires the path to the tree and data files as parameters and
accepts an optional `output` parameter that points to the location where the
result should be stored.

#### Options

* **`--module-tree`** **`-t`**: Path to the XML file containing the module tree information.
* **`--module-data`** **`-d`**: Path to the XML file containing the module, unit and session data.
* **`--output`** **`-o`**: Optional argument providing the path where the created dabase is stored. Defaults to `data.sqlite3` in the working directory.
* **`--help`** **`-h`**: Print usage information.
 
### Bugs

Report bugs [here](http://tuatara.cs.uni-duesseldorf.de/slottool/mincer/issues)

## Building

Building on a headless linux system requires a virtual X11 server like xvfb

Run `lein uberjar` to create a standalone version of the tool that is bundles
with its dependencies.

## License

Copyright © 2015 David Schneider and Philip Höfges.

Distributed under the [ISC License](LICENSE).
