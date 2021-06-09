[![CI](https://github.com/tammoippen/xls2json/actions/workflows/main.yml/badge.svg?branch=main)](https://github.com/tammoippen/xls2json/actions/workflows/main.yml)

# xls2json

Read in Excel file (.xls, .xlsx, .xlsm) and output JSON. Evaluates formulas where possible. Preserve type information from Excel via JSON types.

```sh
❯ xls2json --help
Open an xls(x|m) file and transform to json.

Usage: xls2json [-hlmsvV] [-D=<dtfmt>] [-T=<tfmt>] [-t=<tables>]... [<files>...]

  If no `--table`s are provided, then all
  tables will be extracted.

  All files will be processed one by one,
  each outputting on line of json.

      [<files>...]           xls(x|m)-file(s) to transform
  -h, --help                 Show this help message and exit.
  -V, --version              Print version information and exit.
  -m, --memory               Show memory usage information.
  -v, --verbose              Show more information.
      --pretty               Pretty print the JSON.
  -l, --list-tables          List all tables.
  -t, --table=<tables>       Specify the tables to transform
  -p, --password=<password>  Password for opening the input file(s).
  -s, --strip                Strip empty columns and empty rows.
  -D, --datetime-format=<dtfmt>
                             The datetime format.
                             [default: 'yyyy-MM-dd'T'HH:mm:ss.SSS']
  -T, --time-format=<tfmt>   The time format.
                             [default: 'HH:mm:ss.SSS']

By Tammo Ippen <tammo.ippen@posteo.de>
Issues: https://github.com/tammoippen/xls2json/issues

```

## Usage

```sh
# read a XLS file
❯ ./dist/xls2json-amd64 src/test/resources/empty.xls
{"Sheet1":[]}

# read a XLS file with content
❯ xls2json src/test/resources/sample.xls
{"Sheet1":[["empty",null],["String","hello",null],["StringNumber","14.8",null],["Int",1234,null,null,null,null],["bool",true,null,null,null,null],["bool",false,null,null,null,null],["float",23.12345,null,null,null,null],["datetime","2021-05-18T21:19:53.040",null,null,null,null],["time","21:19:32.000",null],["formulars",null],["string","hello",null],["float",-0.34678748622465627,null],["int",5,null],["datetime","2021-06-03T16:45:56.709",null],["time","13:37:00.000",null],[],[],[],[null,null,null],[null,null,null],[null,null,null]]}

# strip empty cells from from the end of columns and empty rows from the bottom
❯ xls2json -s src/test/resources/sample.xls
{"Sheet1":[["empty"],["String","hello"],["StringNumber","14.8"],["Int",1234],["bool",true],["bool",false],["float",23.12345],["datetime","2021-05-18T21:19:53.040"],["time","21:19:32.000"],["formulars"],["string","hello"],["float",-0.34678748622465627],["int",5],["datetime","2021-06-03T16:47:22.789"],["time","13:37:00.000"]]}

# pretty print the output
❯ xls2json -s --pretty src/test/resources/sample.xls
{
  # we have a dict sheetname -> list of rows
  "Sheet1" : [
    # each row is a list of cell-values
    [ "empty" ],
    [ "String", "hello" ],
    [ "StringNumber", "14.8" ],
    [ "Int", 1234 ],
    [ "bool", true ],
    [ "bool", false ],
    [ "float", 23.12345 ],
    # format for datetime and time can be specified using
    # the -D and -T options. The format-string documentation:
    # https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/time/format/DateTimeFormatter.html
    # Please be aware, that Excel dates have no timezone attached,
    # so if you provide a format with timezone, your locally configured
    [ "datetime", "2021-05-18T21:19:53.040" ],
    [ "time", "21:19:32.000" ],
    # formulars are evaluated
    [ "formulars" ],
    [ "string", "hello" ],
    [ "float", -0.34678748622465627 ],
    [ "int", 5 ],
    [ "datetime", "2021-06-09T21:30:44.166" ],
    [ "time", "13:37:00.000" ]
  ]
}


# use jq (https://stedolan.github.io/jq/)
# or gojq (https://github.com/itchyny/gojq)
# to get some nice output and / or to process the json
❯ xls2json -s src/test/resources/sample.xls | jq ".Sheet1[2]"
[
  "StringNumber",
  "14.8"
]

# XLSX works the same
❯ xls2json -s src/test/resources/sample.xlsx
{"Sheet1":[["empty"],["String","hello"],["StringNumber","14.8"],["Int",1234],["bool",true],["bool",false],["float",23.12345],["datetime","2021-05-18T21:19:53.040"],["time","21:19:32.000"],["formulars"],["string","hello"],["float",-0.34678748622465627],["int",5],["datetime","2021-06-03T16:59:06.466"],["time","13:37:00.000"]]}

# works on multiple files as well
❯ xls2json -s src/test/resources/sample.xls*
{"Sheet1":[["empty"],["String","hello"],["StringNumber","14.8"],["Int",1234],["bool",true],["bool",false],["float",23.12345],["datetime","2021-05-18T21:19:53.040"],["time","21:19:32.000"],["formulars"],["string","hello"],["float",-0.34678748622465627],["int",5],["datetime","2021-06-03T16:59:06.466"],["time","13:37:00.000"]]}
{"Sheet1":[["empty"],["String","hello"],["StringNumber","14.8"],["Int",1234],["bool",true],["bool",false],["float",23.12345],["datetime","2021-05-18T21:19:53.040"],["time","21:19:32.000"],["formulars"],["string","hello"],["float",-0.34678748622465627],["int",5],["datetime","2021-06-03T16:47:22.789"],["time","13:37:00.000"]]}

# list the available tables
❯ xls2json -l src/test/resources/sampleTwoSheets.xls
["Sheet1","Sheet2"]

# only output some table(s)
❯ xls2json -t Sheet2 -s --pretty src/test/resources/sampleTwoSheets.xls
{
  "Sheet2" : [
    [ "empty" ],
    [ "String", "hello" ],
    [ "StringNumber", "14.8" ],
    [ "Int", 1234 ],
    [ "bool", true ],
    [ "bool", false ],
    [ "float", 23.12345 ],
    [ "datetime", "2021-05-18T21:19:53.040" ],
    [ "time", "21:19:32.000" ],
    [ "formulars" ],
    [ "string", "hello" ],
    [ "float", -0.34678748622465627 ],
    [ "int", 5 ],
    [ "datetime", "2021-06-09T21:37:05.615" ],
    [ "time", "13:37:00.000" ]
  ]
}

# if you are running the native-image executable, you can configure the
# garbage collector with the usual options
❯ xls2json -Xmx1k --help
Exception in thread "main" java.lang.OutOfMemoryError: Garbage-collected heap size exceeded.
# 1kb is a bit low

# if you are running with the installable distribution, you can set
# the environment variable XLS2JSON_OPTS
❯ XLS2JSON_OPTS="-Xmx1k" xls2json --help
Error occurred during initialization of VM
Too small maximum heap
# still 1kb is too small :D
```

## Output JsonSchema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "minProperties": 1,
  "patternProperties": {
    ".*": {
      "type": "array",
      "items": {
        "type": "array",
        "items": {
          "type": ["number", "string", "integer", "boolean", "null"]
        }
      }
    }
  }
}
```

## Installation

The [Releases](https://github.com/tammoippen/xls2json/releases) contain various formats for installation:

- An executable fat jar (build with the [shadowJar plugin](https://github.com/johnrengelman/shadow)). Put the jar wherever you like and call it like:

  ```sh
  ❯ java -jar path/to/xls2json-1.0.0-all.jar --help
  ```

- An installable distribution as .zip / .tar.gz. Extract it, put it wherever you like and update your `PATH`, e.g.:

  ```sh
  ❯ tar xf xls2json-1.0.0.tar
  ❯ mv xls2json-1.0.0 "$HOME/.local/share"
  ❯ export PATH="$PATH:$HOME/.local/share/xls2json-1.0.0/bin"
  ❯ xls2json --help
  ```

- Native executable build with [native-image](https://www.graalvm.org/reference-manual/native-image/) from [GraalVM](https://www.graalvm.org/) using the [native-image plugin from mike_neck](https://github.com/mike-neck/graalvm-native-image-plugin). Put the executable for your operating system into your path and call it like: `xls2json --help`.

  **No JAVA installation needed.**

## Another Excel Reader?

There are already some great Excel readers out there, but most do not satisfy my requirements:

- commandline program with (in the best case) no external dependencies
- XLS (2003 and earlier) and XLSX (2007 and later) support
- evaluate formulas within the spreadsheet
- keep type information from Excel
- open source
- (fast and multi-platform)

The existing programs / libraries I found and why I think they do not satisfy my needs:

- [pyexcel-cli](https://github.com/pyexcel/pyexcel-cli): can do much more in and output formats, but lacks formula evaluation. Same for the corresponding [pyexcel](https://github.com/pyexcel/pyexcel) library.
- [unoconv](https://linux.die.net/man/1/unoconv) and [libreoffice](https://www.libreoffice.org/): can do much more in and output formats, can do formula evaluation, but requires a LibreOffice installation.
- [ssconvert](https://help.gnome.org/users/gnumeric/stable/sect-files-ssconvert.html): requires a [gnumeric](http://www.gnumeric.org/) installation, I am not sure about the capabilities.
- [Spreadsheet::XLSX](https://metacpan.org/pod/Spreadsheet::XLSX): no cli afaik, xlsx only. Also perl is not a language I am eager to learn.
- [PhpSpreadsheet](https://github.com/PHPOffice/phpspreadsheet/): no cli afaik, but from the docs looks as if you can evaluate formulas. But again, PHP is not a language I am eager to learn.

I am already using [Apache POI](https://poi.apache.org/) for quite some time and found it quite complete for my needs. We still have the Java VM dependency, but with [GraalVM native-image](https://www.graalvm.org/reference-manual/native-image/) we can build self-contained executables :tada:.

## Standing on the shoulders of giants

- [Apache POI](https://poi.apache.org/): the Java API for Microsoft Documents
- [Picocli](https://picocli.info/): a mighty tiny command line interface
- [jackson](https://github.com/FasterXML/jackson-databind): Jackson has been known as "the Java JSON library" or "the best JSON parser for Java". Or simply as "JSON for Java".
- [Gradle Build Tool](https://gradle.org/)
- [kotlin](https://kotlinlang.org/): A modern programming language that makes developers happier.
- [GraalVM native-image](https://www.graalvm.org/reference-manual/native-image/): ahead-of-time compile Java code to a standalone executable

Some linkes I found invaluable for this project:

- [Working with Native Image efficiently](https://medium.com/graalvm/working-with-native-image-efficiently-c512ccdcd61b)
- [Picocli on GraalVM: Blazingly Fast Command Line Apps](https://picocli.info/picocli-on-graalvm.html)

## Building and Development

Prerequisits:

- Make sure you have a Java (11+) SDK in your path.
- If you want to build the native binary, use the [GraalVM](https://www.graalvm.org/) as your Java SDK and install native-image: `gu install native-image`

Building:

```sh
❯ git clone https://github.com/tammoippen/xls2json.git
❯ cd xls2json
# build all java related targets
# => build/distributions/xls2json-{version}.[tar|zip]
❯ ./gradlew build
# build the native executable for your system
# => build/executable/xls2json
❯ ./gradlew nativeImage
# build the fat jar only (already in build)
# => build/libs/xls2json-{version}-all.jar
❯ ./gradlew shadowJar
# tests and run
❯ ./gradlew test
❯ ./gradlew run --args="--help"
```

## Issues with the native-image executables

Apache POI uses many resources and reflections when working with the excel files and the native-image executable needs to be configured during building to include those resources and reflections.

When issues arise reading an Excel file, please try to use the fat-jar or the installable distribution and run the Excel file with that. If it works, please open an issue with the failing Excel file (or a minimal reproducing Excel file). I will generate the configuration then. If it still does not work, please again open an issue including the Excel file, stack trace, xls2json and java version.

If you want to generate the configuration, consider running:

```sh
❯ ./gradlew shadowJar
❯ $(JAVA_HOME)/bin/java -agentlib:native-image-agent=config-merge-dir=native-image-config \
    -jar build/libs/xls2json-1.0.0-all.jar \
    the-problematic-excel-file.xls(x)
```
