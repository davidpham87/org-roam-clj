(ns org-roam-clj.tags
  "Automatically add links to org file with roam keys.

  Assumption:

  - files and paths are absolute.
  - The database is immutable for the life of the process."
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.zip]
   [org-roam-clj.db]
   [org-roam-clj.utils :refer (->relative-path gather-org-files)]
   [orgmode.core :as org]
   [orgmode.inline]))

(def root-location (System/getProperty "user.dir"))

(def header-content "See also (generated)")
(def header-generate (str "** " header-content))

(defn get-headlines
  ([z] (get-headlines z #{}))
  ([z coll]
   (if (clojure.zip/end? z)
     coll
     (let [node (clojure.zip/node z)
           coll (case (:type node) :headline (conj coll (:text node)) coll)]
       (recur (clojure.zip/next z) coll)))))

(defn contains-genearted-links? [org-data]
  (let [z (clojure.zip/zipper (some-fn map? vector?) seq (fn [_ c] c) org-data)]
    (contains? (get-headlines z) header-content)))

(defn ->org-header [s level] (str (str/join "" (repeat level "*")) " " s))

(defn enclose [text opener closer]
  (str opener text closer))

(defn link
  ([uri] (link uri nil))
  ([uri alias]
   (-> (str (enclose uri "[" "]") (when alias (enclose alias "[" "]")))
       (enclose  "[" "]"))))

(defn ->file-link [root filename alias]
  (link (str "file:" (->relative-path root filename)) alias))

(defn itemize [coll]
  (->> coll
       (map #(str "- " %))
       (str/join "\n" )))


;; TODO: check if this works.
(defn get-tags [x]
  (when-let [tags (get-in x [:attribs :roam-tags])]
    (str/split tags #"\s+")))

(def titles (memoize org-roam-clj.db/titles))
(def backlinks (memoize org-roam-clj.db/backlinks))
(def tags (memoize org-roam-clj.db/tags))

(def filename->title
  (memoize
   (fn []
     (reduce #(assoc %1 (:file %2) (first (:titles %2))) {} (titles)))))

(def tags->filename
  (let [xf (comp
            (map #(update % :titles (fn [s] (mapv str/lower-case s))))
            (map #(zipmap (:titles %) (repeat [(:file %)]))))]
    (transduce xf (fn ([m] m) ([x y] (merge-with into x y))) {} (titles))))

(defn parse-org-link [s]
  (-> (orgmode.inline/make-elem
       [s] orgmode.inline/link-re
       orgmode.inline/link-create)
      first
      (as-> $ (assoc $ :alias (-> $ :content first)))))

(defn file->link [file tags->filename]
  (let [canonical-path (fn [file] (.getCanonicalPath file))
        file (canonical-path file)
        query-tags (fn [tags] (for [t (sort tags) :when t] [t (get tags->filename t)]))
        org-links
        (fn [xs]
          (->> (for [[t links] xs :when (and t links)]
                 (for [link links :when (not= link file)]
                   (->file-link (canonical-path (.(io/file file) getParentFile))
                                (canonical-path (io/file link))
                                ((filename->title) link))))
               (apply concat)
               (into #{})
               (sort-by #(->> % parse-org-link :alias str/lower-case))))]
    (->> (str file)
         (get (tags))
         query-tags
         org-links)))

(defn file->backlink [file]
  (let [canonical-path (fn [filename] (.getCanonicalPath (io/file filename)))]
    (->> (get (backlinks) (str file))
         (map :from)
         (remove #{file})
         (map #(vector % ((filename->title) %)))
         (into #{})
         (sort-by #(-> % second (or "") str/lower-case))
         (mapv #(->file-link (canonical-path (. (io/file file) getParentFile))
                             (canonical-path (io/file (first %))) (second %))))))

(defn append-generated-links
  ([file links] (append-generated-links file links (org/parse file)))
  ([file links org-data]
   (let [org-links (str "\n" (itemize links))
         headline (->org-header header-content 2)]
     (if (contains-genearted-links? org-data)
       (with-open [reader (io/reader (str file))]
         (let [line-reader (line-seq reader)
               [content-before content-after] (-> #(not (str/starts-with? % headline))
                                                  (split-with line-reader))
               content-after (->> (drop 1 content-after)
                                  (drop-while #(not (str/starts-with? % "*"))))]
           (-> (concat content-before [headline org-links] ["\n"] content-after)
               vec
               (as-> $ (str/join "\n" $))
               (as-> $ (assoc {:append false} :text $ :file file)))))
       {:file file :append true :text (str "\n\n" headline "\n" org-links)}))))

(defn create-tag [f]
  (let [file (io/file f)
        org-data (org/parse file)
        link (->> (file->backlink (.getCanonicalPath file))
                  (into (file->link file tags->filename))
                  (into #{})
                  (sort-by #(-> % parse-org-link :alias str/lower-case)))
        text-data (if (seq link) (append-generated-links file link org-data) {})]
    text-data))

(defn clear-generated-tags [f]
  (let [file (io/file f)
        org-data (org/parse file)
        text-data (append-generated-links file [""] org-data)]
    text-data))

(defn save-text! [{:keys [file text append]}]
  (when file
    (spit file text :append append)))

(defn append-tags [fs]
  (doseq [f fs] (-> f create-tag save-text!)))

(defn clear-tags
  ([] (clear-tags "."))
  ([root]
   (let [fs (gather-org-files root)]
     (doseq [f fs]
       (-> f clear-generated-tags save-text!)))))

(defn create-tags
  ([] (create-tags "."))
  ([root]
   (let [fs (gather-org-files root)]
     (doseq [f fs] (-> f create-tag save-text!)))

   ))

(comment

  (append-tags (gather-org-files))
  (clear-tags (gather-org-files))

  (file->backlink "/home/david/Documents/org_files/cards/python.org")

  (->relative-path root-location "/home/david/Documents/org_files/cards/clojure.org")
  (println (:text (create-tag "/home/david/Documents/org_files/decks/clojure.org")))
  {:type :headline
   :text "See also (generated)"
   :level 2}

  (link "https://google.com") ;; => "[[https://google.com]]"
  (link "https://google.com" "google") ;; => "[[https://google.com][google]]"
  (->file-link root-location "/home/david/Documents/org_files/cards/clojure.org" "clj")
  (itemize (range 3)) ;; => "- 0\n- 1\n- 2"

  (-> (get (org/parse (last fs)) :content) last :content last)
  (contains-genearted-links? (org/parse (last fs)))
  (zipmap
   (map str fs)
   (into [] (comp (map org/parse) (map get-tags)) fs))


  (org/parse (nth fs 3)) ;; do some errors parsing here
  ;; append generated-links
  ;; (take-while #(not (str/starts-with "** See also (generated)" %)) line-seq)
  ;; overwrite generated-links (spit :append)


  (let [file (io/file "./todo.org")
        org-data (org/parse file)
        link (file->link file tags->filename)]
    (tap> (vec link))
    #_(when (seq link)
      (def x (append-generated-links file link org-data))))

  (org/parse (nth fs 10))
  (def org-data (org/parse (io/file "./index.org")))
  (def z (clojure.zip/zipper (some-fn map? vector?)
                             seq
                             (fn [_ c] c)
                             org-data))

  (get-headlines z [])
  (= (select-keys (parse-org-link "[[a][abcd]]") [:alias :uri]) {:uri "a", :alias "abcd"})

  (re-seq #"\[[^]]+]" "[[file:cards/clojure.org][Clojure]]")
  (conj #{1 2} 3)

  ;; TODO(dph) add the back links using the links table
  ;; (clojure.core/require '[shadow.cljs.devtools.server :as server])
  ;; (server/start!)

  )
