# Parity fixtures

`ParityTest` formats each file in `input/` with the plugin and compares the
result to the google-java-format CLI's output in `google/` and `aosp/`. Files
end in `.txt` so this repo's own formatter leaves them alone.

The inputs are four files from
[commons-lang](https://github.com/apache/commons-lang) (Apache-2.0, commit
`45c80d99559017ea6efcb5db6156bb9a5c850d34`) and a hand-written `Messy.java`.

The expected outputs come from the google-java-format CLI pinned in `mise.toml`.
From this directory:

```sh
for style in google aosp; do
  tmp=$(mktemp -d)
  for f in input/*.txt; do cp "$f" "$tmp/$(basename "$f" .txt)"; done
  mise x -- google-java-format --replace "--style=$style" "$tmp"/*
  for f in "$tmp"/*; do cp "$f" "$style/$(basename "$f").txt"; done
done
```

Regenerate them when bumping google-java-format.
