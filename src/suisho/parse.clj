(ns suisho.parse
  "Inline Cosense notation -> Markdown conversion."
  (:require [clojure.string :as str]))

;; Filesystem-unsafe characters mapped to their full-width counterparts so
;; that page titles survive as file names and wikilinks keep resolving.
(def ^:private fullwidth
  {"/" "／" "\\" "＼" ":" "：" "*" "＊" "?" "？"
   "\"" "＂" "<" "＜" ">" "＞" "|" "｜"})

(defn sanitize-title
  "Turn a page title into a safe file name (sans extension)."
  [title]
  (-> title
      (str/replace #"[/\\:*?\"<>|]" #(fullwidth %))
      (str/replace #"^[\s.]+|[\s.]+$" "")))

(def ^:private image-url-re #"(?i)\.(png|jpe?g|gif|svg|webp)$")

(defn- gyazo-image-url [url]
  (when-let [[_ id] (re-matches #"https?://gyazo\.com/(\w+)" url)]
    (str "https://i.gyazo.com/" id ".png")))

(defn- link-or-image [url label]
  (if (re-find image-url-re url)
    (str "![" label "](" url ")")
    (str "[" label "](" url ")")))

(defn- decoration->md
  "Wrap text according to Cosense decoration marks (e.g. \"*/\" in [*/ x])."
  [marks text]
  (letfn [(wrap [t mark] (str mark t mark))]
    (cond-> text
      (str/includes? marks "/") (wrap "*")
      (str/includes? marks "*") (wrap "**")
      (str/includes? marks "-") (wrap "~~"))))

(defn- bracket->md
  "Convert the content of a single [bracket] span."
  [content]
  (or
   ;; [$ formula] -> inline math
   (when-let [[_ formula] (re-matches #"\$ (.+)" content)]
     (str "$" formula "$"))
   ;; [* bold] [/ italic] [- strike] and combinations
   (when-let [[_ marks text] (re-matches #"([*/_-]+) (.+)" content)]
     (decoration->md marks text))
   ;; [url] -> embed for images, autolink otherwise
   (when (re-matches #"https?://\S+" content)
     (if-let [img (or (gyazo-image-url content)
                      (when (re-find image-url-re content) content))]
       (str "![](" img ")")
       (str "<" content ">")))
   ;; [url label] / [label url]
   (when-let [[_ url label] (re-matches #"(https?://\S+) (.+)" content)]
     (link-or-image url label))
   (when-let [[_ label url] (re-matches #"(.+) (https?://\S+)" content)]
     (link-or-image url label))
   ;; [/project/page] -> link to the other Scrapbox project
   (when-let [[_ path] (re-matches #"/(\S+)" content)]
     (str "[" path "](https://scrapbox.io/" path ")"))
   ;; [page.icon] -> plain wikilink to the page
   (when-let [[_ page] (re-matches #"(.+)\.icon" content)]
     (str "[[" (sanitize-title page) "]]"))
   ;; [page] -> wikilink, sanitized to match the generated file name
   (str "[[" (sanitize-title content) "]]")))

(defn- convert-text-segment [s]
  (-> s
      ;; [[text]] is Cosense strong emphasis, not a wikilink
      (str/replace #"\[\[([^\[\]]+)\]\]" (fn [[_ text]] (str "**" text "**")))
      (str/replace #"\[([^\[\]]+)\]" (fn [[_ content]] (bracket->md content)))))

(def ^:private code-span-re
  ;; Backtick code spans, runs of other text, or a stray backtick.
  #"`[^`]*`|[^`]+|`")

(defn convert-inline
  "Convert inline Cosense notation in one line to Markdown.
  Content inside backtick code spans is left untouched."
  [line]
  (->> (re-seq code-span-re line)
       (map #(if (str/starts-with? % "`") % (convert-text-segment %)))
       (apply str)))

(defn extract-tags
  "Collect #hashtags from one line, ignoring code spans."
  [line]
  (->> (re-seq code-span-re line)
       (remove #(str/starts-with? % "`"))
       (mapcat #(re-seq #"(?:^|\s)#([^\s\[\]`#]+)" %))
       (mapv second)))
