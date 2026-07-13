# Fixed Default Oracle Config Path

## Goal

Use `C:\Douzone\dews-web\config\modelconvertor\oracle.properties` as the default Oracle properties file.

## Design

- Change only `OracleConfig.defaultPath()`.
- Keep `--config <path>` as an explicit override.
- Keep property loading, validation, and JDBC connection behavior unchanged.
- Update the existing default-path unit test to assert the fixed Windows path.

## Success Criteria

- Running without `--config` loads the fixed path.
- Running with `--config` still loads the supplied path.
- The Maven test suite passes.
