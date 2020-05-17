# org-roam-clj.db

Data converters for org-roam-db



??? tip  "(`ns`)"

    ```clojure
    (ns org-roam-clj.db
      (:require
       [clojure.java.io :as io]
       [clojure.edn]
       [clojure.string :as str]
       [next.jdbc :as jdbc]
       [next.jdbc.result-set :as rs]))
    ```

## `db`



??? tip  "(`def`)"

    ```clojure
    (def db
      {:dbtype "sqlite"
       :dbname "org-roam.db"})
    ```

## `data-resource`

```clojure
(data-resource)
(data-resource db)
```

??? tip  "(`defn`)"

    ```clojure
    (defn data-resource
      ([] (data-resource db))
      ([db] (jdbc/get-datasource db)))
    ```

## `home-directory`



??? tip  "(`def`)"

    ```clojure
    (def home-directory
      (memoize (fn [] (System/getProperty "user.home"))))
    ```

## `execute-query!`

```clojure
(execute-query! query)
(execute-query! query ds)
```

??? tip  "(`defn`)"

    ```clojure
    (defn execute-query!
      ([query] (execute-query! query (data-resource)))
      ([query ds]
       (with-open [connection (jdbc/get-connection ds)]
         (jdbc/execute! connection query {:builder-fn rs/as-unqualified-maps}))))
    ```

## `expand-home`

Inspired by this answer https://stackoverflow.com/questions/29585928/how-to-substitute-path-to-home-for.

```clojure
(expand-home s)
```

??? tip  "(`defn`)"

    ```clojure
    (defn expand-home
      [s]
      (if (.startsWith s "~")
        (clojure.string/replace-first s "~" (home-directory))
        s))
    ```

## `canonical-path`

```clojure
(canonical-path s)
```

??? tip  "(`defn`)"

    ```clojure
    (defn canonical-path [s]
      (.getCanonicalPath (io/file s)))
    ```

## `parse-lisp-record`

```clojure
(parse-lisp-record m)
```

??? tip  "(`defn`)"

    ```clojure
    (defn parse-lisp-record [m]
      (reduce-kv
       (fn [m k v] (assoc m k (try (clojure.edn/read-string v)
                                   (catch Exception _ (println v) v))))
       {} m))
    ```

## `titles`

!!! danger  "Parsing error"

    The displayed code is not valid. This is due to Marginalia's parsing code.





??? tip  "(`defn`)"

    ```clojure
    (defn titles []
      (let [xs (execute-query! ["select * from titles"])]
        (into [] (comp (filter #(str/ends-with? (:file %) ".org\ ))
                       (map parse-lisp-record)
                       (map #(update % :file expand-home))) xs)))
    ```

## `backlinks`

```clojure
(backlinks)
```

??? tip  "(`defn`)"

    ```clojure
    (defn backlinks []
      (->> (execute-query! ["select \"to\", \"from\" from links"])
           (map parse-lisp-record)
           (map #(reduce (fn [m k] (update m k expand-home)) % [:to :from]))
           (group-by :to)))
    ```

## Rich Comment

```clojure
(comment
  (def ds (data-resource))
  (def ts (titles))
  (->> titles
       (map clojure.edn/read-string)
      #_(filter #(str/endswith (:file %) ".org")))
  (group-by :to (map parse-lisp-record (execute-query! ["select \"to\", \"from\" from links"])))
  (clojure.pprint/pprint (titles))
  (clojure.pprint/pprint (backlinks)))
```

