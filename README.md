# ModelConvertor

ModelConvertor is a Windows command-line tool that inspects an Oracle `SELECT` and generates a Java 8 DZ model. It uses JDBC result metadata for field order and types and Oracle column comments for descriptions when a selected item maps unambiguously to a source column.

## Installation

Java 8 or newer is required. Build with `mvn clean package`, then copy `target\modelconvertor.jar` and place these files together:

```text
C:\tools\modelconvertor\
  modelconvertor.jar
  modelconvertor.cmd
  ojdbc8.jar
```

The Oracle driver is deliberately external and is not bundled in the application JAR. Run commands from `cmd.exe` or PowerShell with `C:\tools\modelconvertor` on `PATH`, or invoke `modelconvertor.cmd` by its full path.

Create `%USERPROFILE%\.modelconvertor\oracle.properties`:

```properties
oracle.url=jdbc:oracle:thin:@127.0.0.1:1521/ORCL
oracle.username=MY_USER
oracle.password=MY_PASSWORD
oracle.schema=MY_SCHEMA
```

Use `--config <path>` to load a different properties file. Do not commit this file or share it: it contains a plaintext password. ModelConvertor does not print connection details or passwords, but filesystem permissions still need to protect the file. Use a dedicated read-only Oracle account; SQL validation is a safety check, not a substitute for database privileges.

## Usage

Run `modelconvertor --help` for the complete option summary:

```text
modelconvertor [--sql-file <path>] [--class-name <name>] [--package <name>]
               [--output <source-root>] [--config <path>] [--overwrite]
               [--stdout] [--help]
```

With no arguments in an interactive terminal, enter the class name and package, paste SQL, then enter `:end` on its own line:

```text
modelconvertor
Class name: EmployeeModel
Package name: com.company.hr.model
Paste SQL; enter :end on its own line to finish.
SELECT EMP_NO, EMP_NM FROM HR_EMP
:end
```

Read SQL from a UTF-8 file:

```powershell
modelconvertor --sql-file C:\queries\employee.sql `
  --class-name EmployeeModel --package com.company.hr.model
```

Read SQL from standard input (class and package are required for piped input):

```powershell
Get-Content C:\queries\employee.sql |
  modelconvertor --class-name EmployeeModel --package com.company.hr.model
```

There is intentionally no `--sql` option; use direct paste, a file, or stdin so shell quoting cannot change the SQL.

## Output

By default, UTF-8 source is written under the current directory:

```text
src/main/java/<package path>/<ClassName>.java
```

`--output <source-root>` changes `src/main/java`. Parent directories are created automatically. Existing files cause an error; `--overwrite` explicitly permits replacement. `--stdout` prints generated source and creates no file, which is useful for review or redirection.

## SQL support and limitations

- Only a single `SELECT` or `WITH ... SELECT` is accepted. A trailing semicolon and SQL comments are normalized.
- `INSERT`, `UPDATE`, `DELETE`, `MERGE`, DDL, and multiple statements are rejected.
- MyBatis dynamic tags and placeholders such as `#{...}` and `${...}` are not supported.
- This is a focused SQL inspector, not a complete Oracle parser. Complex expressions should have an explicit, unique `AS` alias.
- Metadata normally comes from `PreparedStatement.getMetaData()`. If the driver cannot provide it, the tool may run a zero-row wrapper (`WHERE 1 = 0`) to obtain metadata; query result rows are never consumed for model generation.
- Every JDBC result column is generated, including `RN` and `ROWNUM`; exclude it in the final select list if unwanted.
- Duplicate result labels or duplicate normalized Java field names stop generation. Add unique aliases.
- Direct source columns can receive `ALL_COL_COMMENTS` descriptions. Computed or ambiguous expressions receive an empty description; comments that are unavailable or inaccessible do not stop generation.
- JDBC types are mapped to practical Java types. Text becomes `String`, decimal numbers may become `BigDecimal`, and Oracle date/timestamp values become `LocalDateTime`; unknown types fall back to `String`.
- The generated model imports DZ and Gson types. Those libraries must exist in the target project where the generated source is compiled.
- Oracle is the only supported database. GUI, automatic updates, and packaged JDBC drivers are outside this tool's scope.
