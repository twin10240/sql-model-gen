---
name: modelconvertor
description: Use when generating a Java DZ model from an Oracle SELECT query, SQL text, or a SQL file with ModelConvertor.
---

# ModelConvertor

Use the existing `modelconvertor.cmd` directly. Do not create another wrapper, parser, or installer.

1. Verify a user-provided text-file path exists before passing it as `--sql-file`; if verification is unavailable, ask instead of assuming. Treat provided SQL text as standard input; use a temporary UTF-8 file only when multiline piping is unsafe, then delete it.
2. Before running, ask only for a missing class name or package name. Pass both with `--class-name` and `--package`.
3. Use `--stdout` for preview-only requests, `--output` for an explicit source root, and `--overwrite` only when the user explicitly requests replacement.
4. Do not alter the SQL or request an Oracle password in chat or command arguments.
5. If the command is missing or installation fails, point to the repository `README.md`; do not add preflight components.
6. On a nonzero exit, report the exit code and stderr without omitting or rewriting the error, and do not retry automatically. The CLI masks its configured password in Oracle/JDBC errors.
