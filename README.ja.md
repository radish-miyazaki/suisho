# suisho

[English](README.md) | 日本語

[Cosense](https://scrapbox.io/)(旧 Scrapbox)のエクスポート JSON を
[Quartz 5](https://quartz.jzhao.xyz/) 用の Markdown に変換する
[Babashka](https://babashka.org/) 製 CLI です。

名前の「suisho(水晶)」は Quartz の和名。日本発のサービス Cosense から
Quartz へページを運ぶツールにふさわしい名前として付けました。

## インストール

[bbin](https://github.com/babashka/bbin) を使い、ローカルのチェックアウトから:

```sh
bbin install .
```

Babashka 1.3.0 以降が必要です。それ以外の依存はありません。
(`deps.edn` は bbin の `:local/root` 解決がマニフェストを要求するために
置いてあるだけで、ツール自体は Babashka 組み込みライブラリのみで動作します)

## 使い方

1. Cosense のプロジェクト設定を開き、
   **Export pages → Export as JSON** を選択します。
2. suisho を実行します:

```sh
suisho export.json -o path/to/quartz/content
```

オプション:

| オプション | 説明 |
| --- | --- |
| `-o, --output DIR` | 出力ディレクトリ(デフォルト: `content`) |
| `--version` | バージョンを表示 |
| `-h, --help` | ヘルプを表示 |

各ページはタイトルをファイル名とする 1 つの `.md` ファイルになり、
そのまま Quartz サイトの `content/` ディレクトリに置けます。

## 変換される記法

| Cosense | Markdown |
| --- | --- |
| `[ページ]` | `[[ページ]]` wikilink(ファイル名に合わせてサニタイズ) |
| `[[強調]]` | `**強調**` |
| `[* 太字]` / `[/ 斜体]` / `[- 打消]` | `**太字**` / `*斜体*` / `~~打消~~` |
| `[** 見出し]` / `[*** 見出し]`(行全体) | `## 見出し` / `# 見出し` |
| `[$ 数式]` | `$数式$` |
| `[url ラベル]` / `[ラベル url]` | `[ラベル](url)` |
| `[url]` | `<url>`、画像の場合は `![](url)` |
| `[https://gyazo.com/ID]` | `![](https://i.gyazo.com/ID.png)` |
| `[/project/page]` | `https://scrapbox.io/project/page` へのリンク |
| `[ページ.icon]` | `[[ページ]]` |
| `code:name.ext` ブロック | 言語指定付きフェンスコードブロック |
| `table:名前` ブロック | GFM テーブル |
| `>引用` | `> 引用` |
| インデント行 | ネストした箇条書き |
| `#タグ` | frontmatter の `tags` に収集(`code:` ブロックやコードスパン内の `#` は無視) |

ページのメタデータ(`created` / `updated`)は frontmatter の
`date` / `modified` になります。バッククォートのコードスパン内は
常に変換されません。

ファイル名に使えない文字(`/ \ : * ? " < > |`)を含むタイトルは
対応する全角文字に置き換えられ、そのページを指す wikilink にも同じ
変換が適用されるため、リンクグラフは壊れません。

## 開発

```sh
bb test        # テストスイートを実行
bb convert export.json -o out   # ソースから実行
```

## ライセンス

MIT
