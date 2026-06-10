(ns suisho.spec
  "Specs for the Cosense export JSON, validated at the CLI boundary."
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]))

(s/def ::title (s/and string? (complement str/blank?)))
(s/def ::text string?)
(s/def ::line (s/or :text string?
                    :map (s/keys :req-un [::text])))
(s/def ::lines (s/coll-of ::line))
(s/def ::created nat-int?)
(s/def ::updated nat-int?)
(s/def ::page (s/keys :req-un [::title ::lines]
                      :opt-un [::created ::updated]))
(s/def ::pages (s/coll-of ::page))
(s/def ::export (s/keys :req-un [::pages]))

(defn validate-export
  "Return `export` when it conforms to ::export, otherwise throw an
  ex-info whose message explains what is wrong with the data."
  [export]
  (if (s/valid? ::export export)
    export
    (throw (ex-info (str "invalid export data: "
                         (s/explain-str ::export export))
                    {:explain (s/explain-data ::export export)}))))
