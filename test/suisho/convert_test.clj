(ns suisho.convert-test
  (:require [clojure.test :refer [deftest is testing]]
            [suisho.convert :as convert]))

(deftest plain-lines-test
  (testing "plain lines become separate paragraphs"
    (is (= "one\n\ntwo" (convert/lines->markdown ["one" "two"]))))
  (testing "blank lines collapse"
    (is (= "one\n\ntwo" (convert/lines->markdown ["one" "" "two"])))))

(deftest indent-list-test
  (testing "indented lines become nested bullets"
    (is (= "intro\n\n- a\n  - b\n- c"
           (convert/lines->markdown ["intro" " a" "  b" " c"]))))
  (testing "tabs work as indent units"
    (is (= "- a\n  - b"
           (convert/lines->markdown ["\ta" "\t\tb"])))))

(deftest heading-test
  (testing "whole-line size decorations become headings"
    (is (= "# Top" (convert/lines->markdown ["[*** Top]"])))
    (is (= "## Section" (convert/lines->markdown ["[** Section]"]))))
  (testing "single-star stays bold, inline decorations stay inline"
    (is (= "**bold**" (convert/lines->markdown ["[* bold]"])))
    (is (= "pre **bold** post" (convert/lines->markdown ["pre [** bold] post"])))))

(deftest quote-test
  (is (= "> quoted" (convert/lines->markdown [">quoted"])))
  (is (= "> quoted" (convert/lines->markdown ["> quoted"]))))

(deftest code-block-test
  (testing "code: blocks become fenced code with language from extension"
    (is (= "```js\nconsole.log('hi');\n```\n\nafter"
           (convert/lines->markdown ["code:hello.js"
                                     " console.log('hi');"
                                     "after"]))))
  (testing "extension-less name is used as the language"
    (is (= "```python\nprint(1)\n```"
           (convert/lines->markdown ["code:python" " print(1)"]))))
  (testing "notation inside code blocks is not converted"
    (is (= "```md\n[link]\n```"
           (convert/lines->markdown ["code:md" " [link]"])))))

(deftest table-block-test
  (is (= "| item | price |\n| --- | --- |\n| apple | 100 |\n| mikan | 80 |"
         (convert/lines->markdown ["table:prices"
                                   " item\tprice"
                                   " apple\t100"
                                   " mikan\t80"]))))

(deftest title->filename-test
  (is (= "page.md" (convert/title->filename "page")))
  (is (= "a／b.md" (convert/title->filename "a/b"))))

(deftest page->markdown-test
  (testing "full page with frontmatter; first line (title) is skipped"
    (is (= (str "---\n"
                "title: \"テスト\"\n"
                "date: 2020-09-13\n"
                "modified: 2020-09-14\n"
                "tags:\n"
                "  - foo\n"
                "---\n"
                "\n"
                "hello [[world]]\n"
                "\n"
                "#foo\n")
           (convert/page->markdown {:title "テスト"
                                    :created 1600000000
                                    :updated 1600086400
                                    :lines ["テスト" "hello [world]" "#foo"]}))))
  (testing "map-style lines (metadata export) are supported"
    (is (= (str "---\n"
                "title: \"p\"\n"
                "---\n"
                "\n"
                "body\n")
           (convert/page->markdown {:title "p"
                                    :lines [{:text "p"} {:text "body"}]}))))
  (testing "hashtags inside code: blocks are not extracted as tags"
    (is (= (str "---\n"
                "title: \"p\"\n"
                "tags:\n"
                "  - real\n"
                "---\n"
                "\n"
                "```sh\n#!/bin/bash\nnpm install #install-deps\n```\n"
                "\n"
                "#real\n")
           (convert/page->markdown
            {:title "p"
             :lines ["p"
                     "code:setup.sh"
                     " #!/bin/bash"
                     " npm install #install-deps"
                     "#real"]}))))
  (testing "double quotes in titles are escaped"
    (is (= (str "---\n"
                "title: \"say \\\"hi\\\"\"\n"
                "---\n"
                "\n"
                "x\n")
           (convert/page->markdown {:title "say \"hi\""
                                    :lines ["say \"hi\"" "x"]})))))
