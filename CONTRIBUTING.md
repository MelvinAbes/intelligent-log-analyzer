# Contributing

Create a focused branch, keep changes within one domain concern, and include tests for behavioural
changes.

Before opening a pull request:

```bash
make format
make check
make secret-scan
```

Never use real private logs in fixtures, issues, or examples.
