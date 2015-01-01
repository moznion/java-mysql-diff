mysql-diff [![Build Status](https://travis-ci.org/moznion/java-mysql-diff.svg?branch=master)](https://travis-ci.org/moznion/java-mysql-diff)
==

Detect and extract diff between two table declarations from schema of MySQL.

Synopsis
--

### Use as CLI application

Executable fat-jar is available at [here](https://github.com/moznion/java-mysql-diff/releases).

```
$ java -jar [old_database] [new_database]
```

If you want more details, please run this command with `--help` option.

### Programmatically

```java
import net.moznion.mysql.diff.DiffExtractor;
import net.moznion.mysql.diff.MySqlConnectionInfo;
import net.moznion.mysql.diff.SchemaDumper;
import net.moznion.mysql.diff.SchemaParser;

MySqlConnectionInfo mySqlConnectionInfo = MySqlConnectionInfo.builder()
    .host("localhost") // "localhost" is the default value
    .user("root")      // "root" is the default value
    .pass("")          // "" is the default value
    .build();
SchemaDumper schemaDumper = new SchemaDumper(mySqlConnectionInfo);

String oldSchema = schemaDumper.dump(oldSql);
String newSchema = schemaDumper.dump(newSql);

List<Table> oldTables = SchemaParser.parse(oldSchema);
List<Table> newTables = SchemaParser.parse(newSchema);

String diff = DiffExtractor.extractDiff(oldTables, newTables);
```

Description
--

This package provides a function to detect and extract diff between two table declarations from schema.

Extraction processing flow is following;

1. Dump normalized schema by creating new database according to input and dump it out by using `mysqldump`
2. Parse normalized schema
3. Take diff between parsed structure

This package is port of onishi-san's [mysqldiff](https://github.com/onishi/mysqldiff) from Perl to Java.

Dependencies
--

- Java 8 or later
- MySQL 5 or later (`mysqld` must be upped when this program is executed)
- mysqldump

How to build fat-jar
--

If you want to build an executable standalone jar,
please run following command;

```
$ mvn -P fatjar clean package
```

And now, generated runnable fat-jar file has been available on [GitHub Releases](https://github.com/moznion/java-mysql-diff/releases).

See Also
--

- [mysqldiff](https://github.com/onishi/mysqldiff)

Author
--

moznion (<moznion@gmail.com>)

License
--

```
The MIT License (MIT)
Copyright © 2014 moznion, http://moznion.net/ <moznion@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the “Software”), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
```

