(ns suisho.golden-test
  "Whole-output regression test against the golden fixture.
  Regenerate the expected output with `bb update-golden`."
  (:require [babashka.fs :as fs]
            [clojure.test :refer [deftest is]]
            [suisho.main :as main]))

(def ^:private export-json "test/golden/export.json")
(def ^:private expected-dir "test/golden/expected")

(defn- files-by-name [dir]
  (into {}
        (map (juxt (comp str fs/file-name) (comp slurp fs/file)))
        (fs/list-dir dir)))

(deftest golden-output
  ;; under target/ rather than the system temp dir, which may be unwritable
  ;; in sandboxed environments
  (let [out (fs/create-temp-dir {:dir (fs/create-dirs "target")
                                 :prefix "golden-"})]
    (try
      (main/convert! export-json (str out))
      (let [expected (files-by-name expected-dir)
            actual (files-by-name out)]
        (is (= (set (keys expected)) (set (keys actual)))
            "generated file set differs from golden")
        (doseq [[filename content] (sort expected)]
          (is (= content (get actual filename))
              (str "content differs from golden: " filename))))
      (finally (fs/delete-tree out)))))
