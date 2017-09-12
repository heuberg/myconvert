# MyConvert
**A flexible Android (unit-)conversion app.**

## Features
* Simple design
* No ads, no spying
* Have just the conversions you need
* Define your own conversions (simple XML-based definitions)
* Download others' conversion definitions
* Conversions can be grouped in categories
* Edit mode to edit definitions from within the app
* Duplicate conversion definitions (&rarr; rapid creation of new ones)
* Backup all definitions

## Usage
Conversion definitions are stored in XML files in the app's external storage directory (something like Android/data/com.github.heuberg.myconversion/files/). The app loads all XML files in this directory at startup time. For first time usage you have to put some conversion definition files there.
## Conversion definitions
You can find conversion definition files here (coming soon) or you *duplicate* the default converion (which creates an according file) and edit it via *Edit mode* or an appropriate XML-editor. Check out existing files to find out how to define new conversions.
### Example of a simple conversion definition
```xml
    <?xml version="1.0" encoding="UTF-8"?>
    <myconvert version="1">
        <def name="Degree C -> F" cat="Temperatures">
            <var name="Celsius" def="20.0" />
            <res name="Fahrenheit" formula="v1*1.8+32" />
        </def>
    </myconvert>
```
As you can see, you define conversions with the `<def>` element giving it a `name` attribute and a category (`cat`). You define *variables* with the `<var>` element specifying a `name` attribute and an optional default value (`def`). Results are defined via `<res>` elements with `name` and `formula` attributes. You may define any number of variables and results, but have to define at least one each. (Each file may contain one or more conversion defintions, i.e. `<def>` elements.)
### Formulas
Within formulas you can reference the variables with `v1` (1st variable's value) to `vN` (Nth variable's value).
Formulas may contain *Operators:* `+`, `-`, `\*`, `/`, `^`, `%`. *Functions:* `abs`, `acos`, `asin`, `atan`, `cbrt`, `ceil`, `cos`, `cosh`, `exp`, `floor`, `log`, `log10`, `log2`, `sin`, `sinh`, `sqrt`, `tan`, `tanh`, `signum`. *Constants:* `pi`, `e`. (For evaluation the exp4j library is used: see [objecthunter.net/exp4j](https://objecthunter.net/exp4j))

### Example of a simple conversion definition with rewritten form
In some cases, as in the example above, you maybe want to also convert the other way round, i.e. to rewrite the formula (here: convert from Fahrenheit to Celsius). This can be done by also specifying variables and results for the rewritten form.
```xml
    <?xml version="1.0" encoding="UTF-8"?>
    <myconvert version="1">
        <def name="Degree C -> F" cat="Temperatures">
            <var name="Celsius" def="20.0" />
            <res name="Fahrenheit" formula="v1*1.8+32" />
            <revar name="Fahrenheit" def="68.0" />
            <reres name="Celsius" formula="(v1-32)/1.8" />
        </def>
    </myconvert>
```
As you can see, you define *rewritten* variables and formulas similar to the ones in *standard* form using `<revar>` for variables and `<reres>` for results.

### Notes on the XML-structure
* Conversion definition names have to be **unique**! Otherwise the definition loaded at last is actually used.
* Categories are just strings. There is no translation or whatsoever.
* The order of variable definitions matters. The first definition is referred to as `v1` in formulas, the second as `v2` and so on.
* For now the version attribute has to be `="1"`.

## [Contributing](CONTRIBUTING.md)
For information on how you can contribute see [here](CONTRIBUTING.md).

## [License](..\LICENSE)
This project is published under GNU General Public License v3. It uses the library [exp4j](https://objecthunter.net/exp4j) which is under Apache License 2.0

## Contact
If you have any questions, comments, bugs or anything else you want to share with us, please feel free to contact us by [mail](mailto:hameau@eclipso.at).

`To be continued...`
