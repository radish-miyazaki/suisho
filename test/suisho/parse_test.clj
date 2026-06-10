(ns suisho.parse-test
  (:require [clojure.test :refer [deftest is testing]]
            [suisho.parse :as parse]))

(deftest sanitize-title-test
  (testing "replaces filesystem-unsafe characters with full-width forms"
    (is (= "a／b" (parse/sanitize-title "a/b")))
    (is (= "q&a：faq" (parse/sanitize-title "q&a:faq")))
    (is (= "what？" (parse/sanitize-title "what?")))
    (is (= "a＊b＂c＜d＞e｜f＼g"
           (parse/sanitize-title "a*b\"c<d>e|f\\g"))))
  (testing "trims surrounding whitespace and dots"
    (is (= "page" (parse/sanitize-title " page. ")))))

(deftest strong-notation-test
  (testing "[[text]] is Cosense strong, not a wikilink"
    (is (= "**bold**" (parse/convert-inline "[[bold]]")))
    (is (= "a **b** c" (parse/convert-inline "a [[b]] c")))))

(deftest internal-link-test
  (testing "[page] becomes a wikilink"
    (is (= "[[Some Page]]" (parse/convert-inline "[Some Page]")))
    (is (= "see [[Some Page]] here" (parse/convert-inline "see [Some Page] here"))))
  (testing "wikilink targets are sanitized to match output filenames"
    (is (= "[[a／b]]" (parse/convert-inline "[a/b]")))))

(deftest decoration-test
  (is (= "**bold**" (parse/convert-inline "[* bold]")))
  (is (= "**bigger**" (parse/convert-inline "[** bigger]")))
  (is (= "*italic*" (parse/convert-inline "[/ italic]")))
  (is (= "~~gone~~" (parse/convert-inline "[- gone]")))
  (is (= "***both***" (parse/convert-inline "[*/ both]"))))

(deftest math-test
  (is (= "$x^2 + y^2$" (parse/convert-inline "[$ x^2 + y^2]"))))

(deftest external-link-test
  (testing "url with label, either order"
    (is (= "[Example](https://example.com)"
           (parse/convert-inline "[https://example.com Example]")))
    (is (= "[My Example](https://example.com)"
           (parse/convert-inline "[My Example https://example.com]"))))
  (testing "bare url"
    (is (= "<https://example.com>"
           (parse/convert-inline "[https://example.com]")))))

(deftest image-test
  (testing "direct image urls become embeds"
    (is (= "![](https://example.com/a.png)"
           (parse/convert-inline "[https://example.com/a.png]")))
    (is (= "![photo](https://example.com/a.jpg)"
           (parse/convert-inline "[https://example.com/a.jpg photo]"))))
  (testing "gyazo page urls are rewritten to raw image urls"
    (is (= "![](https://i.gyazo.com/abc123.png)"
           (parse/convert-inline "[https://gyazo.com/abc123]")))))

(deftest cross-project-link-test
  (is (= "[proj/page](https://scrapbox.io/proj/page)"
         (parse/convert-inline "[/proj/page]"))))

(deftest icon-test
  (is (= "[[daisy]]" (parse/convert-inline "[daisy.icon]"))))

(deftest code-span-test
  (testing "content inside backticks is left untouched"
    (is (= "`[not a link]`" (parse/convert-inline "`[not a link]`")))
    (is (= "x `[a]` [[b]]" (parse/convert-inline "x `[a]` [b]")))))

(deftest extract-tags-test
  (is (= ["foo" "ばず"] (parse/extract-tags "text #foo and #ばず end")))
  (is (= [] (parse/extract-tags "no tags, not even в#middle")))
  (testing "tags inside code spans are ignored"
    (is (= [] (parse/extract-tags "`#nope`")))))
