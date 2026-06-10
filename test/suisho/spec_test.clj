(ns suisho.spec-test
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [suisho.spec :as spec]))

(def valid-export
  {:name "proj"
   :pages [{:title "Page One"
            :created 1600000000
            :updated 1600086400
            :lines ["Page One" "hello [Page Two]"]}
           {:title "Page Two"
            :lines [{:text "Page Two"} {:text "body"}]}]})

(deftest export-spec-test
  (testing "a well-formed export is valid"
    (is (s/valid? ::spec/export valid-export)))
  (testing "pages key is required"
    (is (not (s/valid? ::spec/export {:name "proj"}))))
  (testing "a page requires a title"
    (is (not (s/valid? ::spec/export {:pages [{:lines ["x"]}]}))))
  (testing "a page title must not be blank"
    (is (not (s/valid? ::spec/export {:pages [{:title "" :lines ["x"]}]}))))
  (testing "a page requires lines"
    (is (not (s/valid? ::spec/export {:pages [{:title "T"}]}))))
  (testing "lines may mix strings and {:text ...} maps"
    (is (s/valid? ::spec/export
                  {:pages [{:title "T" :lines ["a" {:text "b"}]}]})))
  (testing "a line map requires a string :text"
    (is (not (s/valid? ::spec/export
                       {:pages [{:title "T" :lines [{:text 1}]}]}))))
  (testing "timestamps must be non-negative integers when present"
    (is (not (s/valid? ::spec/export
                       {:pages [{:title "T" :lines ["T"] :created -1}]})))
    (is (not (s/valid? ::spec/export
                       {:pages [{:title "T" :lines ["T"] :updated "soon"}]})))))

(deftest validate-export-test
  (testing "returns the export unchanged when valid"
    (is (= valid-export (spec/validate-export valid-export))))
  (testing "throws ex-info with a human-readable explanation when invalid"
    (let [e (try (spec/validate-export {:pages [{:title "T"}]})
                 (catch clojure.lang.ExceptionInfo e e))]
      (is (instance? clojure.lang.ExceptionInfo e))
      (is (str/includes? (ex-message e) "lines"))
      (is (some? (:explain (ex-data e))))))
  (testing "rejects a non-map export (e.g. wrong JSON shape)"
    (is (thrown? clojure.lang.ExceptionInfo (spec/validate-export [1 2 3])))))
