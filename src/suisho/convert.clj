(ns suisho.convert
  "Cosense page -> Quartz Markdown document."
  (:require [clojure.string :as str]
            [suisho.parse :as parse])
  (:import [java.time Instant ZoneOffset]))

(defn- indent-of [line]
  (count (re-find #"^[ \t]*" line)))

(defn- strip-indent [line n]
  (str/replace line (re-pattern (str "^[ \t]{0," n "}")) ""))

(defn- code-lang
  "Language for a fenced block, from the code:NAME line."
  [name]
  (if-let [[_ ext] (re-find #"\.([^.]+)$" name)]
    ext
    name))

(defn- consume-block
  "Take lines more deeply indented than `base` (the code:/table: line)."
  [base lines]
  (split-with #(> (indent-of %) base) lines))

(defn- code-block [line more]
  (let [base (indent-of line)
        name (subs (str/trim line) (count "code:"))
        [body rest] (consume-block base more)
        body (map #(strip-indent % (inc base)) body)]
    [(str "```" (code-lang name) "\n" (str/join "\n" body) "\n```") rest]))

(defn- table-block [line more]
  (let [base (indent-of line)
        [rows rest] (consume-block base more)
        cells (map #(str/split (strip-indent % (inc base)) #"\t") rows)
        row->md (fn [r] (str "| " (str/join " | " r) " |"))
        [header & body] cells
        sep (str "| " (str/join " | " (repeat (count header) "---")) " |")]
    [(str/join "\n" (concat [(row->md header) sep] (map row->md body))) rest]))

(defn- heading-line
  "Whole-line [*** x] / [** x] at indent 0 becomes a heading."
  [line]
  (when-let [[_ stars text] (re-matches #"\[(\*{2,3}) ([^\[\]]+)\]" line)]
    (str (if (= stars "***") "#" "##") " " (parse/convert-inline text))))

(defn- bullet-line [line]
  (let [level (indent-of line)]
    (str (apply str (repeat (* 2 (dec level)) " "))
         "- "
         (parse/convert-inline (str/trim line)))))

(defn lines->markdown
  "Convert a page body (vector of Cosense lines) to Markdown."
  [lines]
  (loop [[line & more :as all] (seq lines)
         blocks []
         bullets []]
    (let [flushed (cond-> blocks
                    (seq bullets) (conj (str/join "\n" bullets)))]
      (cond
        (nil? all)
        (str/join "\n\n" flushed)

        (str/blank? line)
        (recur more flushed [])

        (str/starts-with? (str/triml line) "code:")
        (let [[block rest] (code-block line more)]
          (recur (seq rest) (conj flushed block) []))

        (str/starts-with? (str/triml line) "table:")
        (let [[block rest] (table-block line more)]
          (recur (seq rest) (conj flushed block) []))

        (pos? (indent-of line))
        (recur more blocks (conj bullets (bullet-line line)))

        (heading-line line)
        (recur more (conj flushed (heading-line line)) [])

        (str/starts-with? line ">")
        (recur more
               (conj flushed (str "> " (parse/convert-inline
                                        (str/triml (subs line 1)))))
               [])

        :else
        (recur more (conj flushed (parse/convert-inline line)) [])))))

(defn title->filename [title]
  (str (parse/sanitize-title title) ".md"))

(defn- epoch->date [epoch]
  (when epoch
    (str (.toLocalDate (.atOffset (Instant/ofEpochSecond epoch) ZoneOffset/UTC)))))

(defn- yaml-escape [s]
  (-> s
      (str/replace "\\" "\\\\")
      (str/replace "\"" "\\\"")))

(defn- line-text [line]
  (if (map? line) (:text line) line))

(defn- without-code-blocks
  "Drop code: blocks (marker and body) so their #text is not read as tags."
  [lines]
  (loop [[line & more :as all] (seq lines)
         out []]
    (cond
      (nil? all)
      out

      (str/starts-with? (str/triml line) "code:")
      (let [[_ rest] (consume-block (indent-of line) more)]
        (recur (seq rest) out))

      :else
      (recur more (conj out line)))))

(defn- frontmatter [{:keys [title created updated]} tags]
  (str "---\n"
       "title: \"" (yaml-escape title) "\"\n"
       (when-let [date (epoch->date created)]
         (str "date: " date "\n"))
       (when-let [date (epoch->date updated)]
         (str "modified: " date "\n"))
       (when (seq tags)
         (str "tags:\n" (apply str (map #(str "  - " % "\n") tags))))
       "---\n"))

(defn page->markdown
  "Render one exported page as a complete Markdown document."
  [{:keys [title lines] :as page}]
  (let [texts (map line-text lines)
        ;; the first line repeats the title; drop it from the body
        body-lines (if (= (first texts) title) (rest texts) texts)
        tags (distinct (mapcat parse/extract-tags
                               (without-code-blocks body-lines)))]
    (str (frontmatter page tags)
         "\n"
         (lines->markdown body-lines)
         "\n")))
