(ns org-roam-clj.db
  "Data converters for org-roam-db"
  (:require
   [clojure.edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [honeysql.core :as sql]
   [org-roam-clj.utils :refer (sha1-str gather-org-files)]
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as rs]))

(def db
  {:dbtype "sqlite"
   :dbname "org-roam.db"})

(defn data-resource
  ([] (data-resource db))
  ([db] (jdbc/get-datasource db)))

(def connection (atom (jdbc/get-connection (data-resource))))

(def home-directory
  (memoize (fn [] (System/getProperty "user.home"))))

(defn execute-query!
  ([query] (execute-query! query @connection))
  ([query connection]
   (jdbc/execute! connection query {:builder-fn rs/as-unqualified-maps})))

(defn expand-home
  "Inspired by this answer https://stackoverflow.com/questions/29585928/how-to-substitute-path-to-home-for."
  [s]
  (if (.startsWith s "~")
    (clojure.string/replace-first s "~" (home-directory))
    s))

(defn canonical-path [s]
  (.getCanonicalPath (io/file s)))

(defn parse-lisp-record [m]
  (reduce-kv
   (fn [m k v] (assoc m k
                      (try (clojure.edn/read-string v)
                           (catch Exception _ (println v)
                                  (str (take-while #(not= "\"") (drop 2 v)))))))
   {} m))

(defn titles []
  (let [xs (execute-query! ["select * from titles"])]
    (into [] (comp (filter #(str/ends-with? (:file %) ".org\"" ))
                   (map parse-lisp-record)
                   (map #(update % :file expand-home))) xs)))

(defn backlinks []
  (->> (execute-query! ["select \"to\", \"from\" from links"])
       (map parse-lisp-record)
       (map #(reduce (fn [m k] (update m k expand-home)) % [:to :from]))
       (group-by :to)))

(defn create-files-clj []
  (try
    (execute-query!
     ["create table files_clj  (
file varchar(3000) PRIMARY KEY,
hash vacharc(40)
)"])
    (catch org.sqlite.SQLiteException _
      (println "Table files_clj already exists."))))

(defn insert-file-hash-query [filename content]
  (let [sha1 (sha1-str content)]
    {:insert-into :files_clj
     :columns [:file :hash]
     :values [[filename sha1]]}))

(defn update-file-hash-query [filename content]
  (let [sha1 (sha1-str content)]
    {:update :files_clj
     :set {:file filename :hash sha1}
     :where [:= :file filename]}))

(defn file-query [filename]
  {:select [:*]
   :from [:files_clj]
   :where [:= :file filename]
   :limit 1})

(defn file-exists? [filename]
  (pos? (count (execute-query! (sql/format (file-query filename))))))

(defn store-file-hash-query
  ([filename content]
   (store-file-hash-query filename content (file-exists? filename)))
  ([filename content update?]
   (if update?
     (update-file-hash-query filename content)
     (insert-file-hash-query filename content))))

(defn store-file-hash!
  ([filename content]
   (store-file-hash! filename content (file-exists? filename)))
  ([filename content update?]
   (-> (store-file-hash-query filename content update?)
       sql/format
       execute-query!)))

(defn store-hash-content! [f]
  (let [filename (.getCanonicalPath f)]
    (store-file-hash! filename (slurp filename))))

(defn build-files-hash [root]
  (doseq [f (gather-org-files root)]
    (store-hash-content! f)))

#_(defn updated-files [files]
  {:select [:*]
   :from [:files_clj]
   :where [:in :file files]})

(comment
  (def ds (data-resource))
  (def ts (titles))

  (->> titles
       (map clojure.edn/read-string)
       #_(filter #(str/endswith (:file %) ".org")))

  (group-by :to (map parse-lisp-record (execute-query! ["select \"to\", \"from\" from links"])))
  (create-files-clj)

  (clojure.pprint/pprint (titles))
  (clojure.pprint/pprint (backlinks))
  (titles)
  (build-files-hash ".")
  (def sql-map {:select [:*] :from [:files]})

  (execute-query! (sql/format sql-map))
  (execute-query! (sql/format sql-map))

  ;; just for fun use datascript and store the atom into a single transit json

  (execute-query! (sql/format  (store-file-hash-query "whatever.clj" "aésldkfjélkjéaldskjfélkj")))
  (execute-query! (sql/format  (update-file-hash-query "hello.clj" "aésldkfjélkjéaldskjfélkj")))
  (execute-query! (sql/format {:select [:*]
                               :from [:files_clj]
                               :limit 10})))
