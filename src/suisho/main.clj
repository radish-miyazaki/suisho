(ns suisho.main
  "CLI entry point: convert a Cosense export JSON into Quartz Markdown."
  (:require [babashka.cli :as cli]
            [babashka.fs :as fs]
            [cheshire.core :as json]
            [suisho.convert :as convert]))

(def version "0.1.0")

(def ^:private usage-text
  (str "suisho " version
       " - convert a Cosense (Scrapbox) export to Quartz Markdown\n"
       "\n"
       "Usage: suisho <export.json> [options]\n"
       "\n"
       "Options:\n"
       "  -o, --output DIR  Output directory (default: content)\n"
       "      --version     Print version\n"
       "  -h, --help        Show this help"))

(def ^:private cli-spec
  {:output {:alias :o :default "content"}
   :help {:alias :h :coerce :boolean}
   :version {:coerce :boolean}})

(defn parse-args [args]
  (let [{:keys [args opts]} (cli/parse-args args {:spec cli-spec})]
    (cond-> {:output (:output opts)}
      (seq args) (assoc :input (first args))
      (:help opts) (assoc :help true)
      (:version opts) (assoc :version true))))

(defn convert!
  "Convert every page in the export JSON at `input` into Markdown files
  under `output`. Returns {:pages <count>}."
  [input output]
  (let [{:keys [pages]} (json/parse-string (slurp input) true)]
    (fs/create-dirs output)
    (doseq [page pages]
      (spit (str (fs/path output (convert/title->filename (:title page))))
            (convert/page->markdown page)))
    {:pages (count pages)}))

(defn- die [message]
  (binding [*out* *err*] (println (str "suisho: " message)))
  (System/exit 1))

(defn -main [& args]
  (let [{:keys [input output help] :as opts} (parse-args args)]
    (cond
      help (println usage-text)
      (:version opts) (println (str "suisho " version))
      (nil? input) (die "no input file given (try --help)")
      (not (fs/exists? input)) (die (str "input file not found: " input))
      :else
      (try
        (let [{:keys [pages]} (convert! input output)]
          (println (str "Converted " pages " page(s) into " output "/")))
        (catch Exception e
          (die (str "failed to convert: " (ex-message e))))))))
