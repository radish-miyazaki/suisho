# suisho

A [Babashka](https://babashka.org/) CLI that converts a
[Cosense](https://scrapbox.io/) (formerly Scrapbox) JSON export into
Markdown for [Quartz 5](https://quartz.jzhao.xyz/).

*Suishō* (水晶) is the Japanese word for quartz — a fitting name for a
tool that carries pages from Cosense, a Japanese-born service, over to
Quartz.

## Installation

With [bbin](https://github.com/babashka/bbin), from a local checkout:

```sh
bbin install .
```

Requires Babashka 1.3.0 or later. No other dependencies.
(`deps.edn` exists only so bbin's `:local/root` resolution finds a
manifest; the tool itself runs on Babashka built-ins alone.)

## Usage

1. In Cosense, open your project settings and choose
   **Export pages → Export as JSON**.
2. Run suisho:

```sh
suisho export.json -o path/to/quartz/content
```

Options:

| Option | Description |
| --- | --- |
| `-o, --output DIR` | Output directory (default: `content`) |
| `--version` | Print version |
| `-h, --help` | Show help |

Each page becomes one `.md` file named after its title, ready to drop
into the `content/` directory of a Quartz site.

## What gets converted

| Cosense | Markdown |
| --- | --- |
| `[Page]` | `[[Page]]` wikilink (sanitized to match file names) |
| `[[strong]]` | `**strong**` |
| `[* bold]` / `[/ italic]` / `[- strike]` | `**bold**` / `*italic*` / `~~strike~~` |
| `[** Heading]` / `[*** Heading]` (whole line) | `## Heading` / `# Heading` |
| `[$ formula]` | `$formula$` |
| `[url label]` / `[label url]` | `[label](url)` |
| `[url]` | `<url>`, or `![](url)` for images |
| `[https://gyazo.com/ID]` | `![](https://i.gyazo.com/ID.png)` |
| `[/project/page]` | Link to `https://scrapbox.io/project/page` |
| `[page.icon]` | `[[page]]` |
| `code:name.ext` blocks | Fenced code blocks with the language |
| `table:name` blocks | GFM tables |
| `>quote` | `> quote` |
| Indented lines | Nested bullet lists |
| `#tag` | Collected into frontmatter `tags` |

Page metadata (`created` / `updated`) becomes `date` / `modified` in the
frontmatter; backtick code spans are always left untouched.

Titles containing characters that are unsafe in file names
(`/ \ : * ? " < > |`) are rewritten to their full-width counterparts,
and wikilinks pointing at them are rewritten the same way so the link
graph stays intact.

## Development

```sh
bb test        # run the test suite
bb convert export.json -o out   # run from source
```

## License

MIT
