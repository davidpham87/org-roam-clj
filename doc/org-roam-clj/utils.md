# org-roam-clj.utils



??? tip  "(`ns`)"

    ```clojure
    (ns org-roam-clj.utils
      (:require
       [clojure.java.io :as io]
       [clojure.string :as str])
      (:import
       [java.net URI]
       [java.nio.file Paths]
       [java.security MessageDigest]))
    ```

## `debug`

```clojure
(debug x)
```

??? tip  "(`defn`)"

    ```clojure
    (defn debug [x]
      (println x)
      x)
    ```

## `now`





```clojure
(now)
```

??? tip  "(`defn`)"

    ```clojure
    (defn now [] (java.time.ZonedDateTime/now))
    (defn now-formatted []
      (.format (now) (java.time.format.DateTimeFormatter/ofPattern "YYYYMMdd hhmmss.SSS: ")))
    ```

## `extension`

```clojure
(extension path)
```

??? tip  "(`defn`)"

    ```clojure
    (defn extension [path] (subs path (str/last-index-of path ".")))
    ```

## `replace-extension`

```clojure
(replace-extension path extension)
```

??? tip  "(`defn`)"

    ```clojure
    (defn replace-extension [path extension]
      (-> path
          (subs 0 (str/last-index-of path "."))
          (str "." extension)))
    ```

## `replace-root`

```clojure
(replace-root path root)
```

??? tip  "(`defn`)"

    ```clojure
    (defn replace-root [path root]
      (str root "/" (str/replace path #"^[^/]*/" )))
    ```

## `->Path`

```clojure
(->Path s)
```

??? tip  "(`defn`)"

    ```clojure
    (defn ->Path [s]
      (->> (str/replace s #"^\." )
           (str "file://")
           (URI.)
           Paths/get
           (.normalize)))
    ```

## `->relative-path`

Returns the relative path of `location` with respect to `root`

```clojure
(->relative-path root location)
```

??? tip  "(`defn`)"

    ```clojure
    (defn ->relative-path
      [root location]
      (-> (.relativize (->Path root) (->Path location))
          (.normalize)
          str))
    ```

## `gather-org-files`

```clojure
(gather-org-files)
(gather-org-files root)
```

??? tip  "(`defn`)"

    ```clojure
    (defn gather-org-files
      ([] (gather-org-files "."))
      ([root]
       (into [] (comp (filter #(str/ends-with? % ".org"))
                      (map #(.getCanonicalPath %)))
             (file-seq (io/file root)))))
    ```

## `sha1-str`

```clojure
(sha1-str s)
```

??? tip  "(`defn`)"

    ```clojure
    (defn sha1-str [s]
      (let [digested (-> "sha1"
                         MessageDigest/getInstance
                         (.digest (.getBytes s)))]
        (->> digested
             (map #(.substring
                    (Integer/toString
                     (+ (bit-and % 0xff) 0x100) 16) 1))
             (apply str))))
    ```

## `gather-org-files`

```clojure
(gather-org-files root)
```

??? tip  "(`defn`)"

    ```clojure
    (defn gather-org-files [root]
      (filterv #(str/ends-with? % ".org") (file-seq (io/file root))))
    ```

