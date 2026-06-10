(ns suisho.main-test
  (:require [babashka.fs :as fs]
            [cheshire.core :as json]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [suisho.main :as main]))

(defn- fixture-export []
  {:name "proj"
   :displayName "Project"
   :pages [{:title "Page One"
            :created 1600000000
            :updated 1600086400
            :lines ["Page One" "hello [Page Two]" " bullet #tagged"]}
           {:title "Page Two"
            :lines [{:text "Page Two"} {:text "body"}]}]})

(defn- temp-root []
  ;; Respect TMPDIR so the test also runs in sandboxed environments
  ;; where java.io.tmpdir and TMPDIR disagree.
  (or (System/getenv "TMPDIR") (str (fs/temp-dir))))

(deftest convert!-test
  (let [tmp (fs/create-temp-dir {:dir (temp-root)})
        in (str (fs/path tmp "export.json"))
        out (str (fs/path tmp "content"))]
    (try
      (spit in (json/generate-string (fixture-export)))
      (testing "returns the number of converted pages"
        (is (= 2 (:pages (main/convert! in out)))))
      (testing "writes one Markdown file per page"
        (is (fs/exists? (fs/path out "Page One.md")))
        (is (fs/exists? (fs/path out "Page Two.md"))))
      (testing "output contains frontmatter and converted notation"
        (let [md (slurp (str (fs/path out "Page One.md")))]
          (is (str/starts-with? md "---\ntitle: \"Page One\"\n"))
          (is (str/includes? md "date: 2020-09-13"))
          (is (str/includes? md "[[Page Two]]"))
          (is (str/includes? md "- bullet"))
          (is (str/includes? md "  - tagged"))))
      (finally
        (fs/delete-tree tmp)))))

(deftest parse-args-test
  (is (= {:input "export.json" :output "content"}
         (main/parse-args ["export.json"])))
  (is (= {:input "export.json" :output "site/content"}
         (main/parse-args ["export.json" "-o" "site/content"])))
  (is (= {:input "export.json" :output "site/content"}
         (main/parse-args ["export.json" "--output" "site/content"]))))
