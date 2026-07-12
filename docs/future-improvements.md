# Future Improvements

## Interactive class and package retry

Interactive mode currently exits with code 2 when the class name or package name is empty or invalid. A future UX improvement may prompt again instead.

Proposed behavior:

- Repeat the prompt for empty or invalid values.
- Show the validation reason before prompting again.
- Treat EOF as cancellation and exit instead of retrying forever.
- Keep piped mode unchanged: `--class-name` and `--package` remain required.
