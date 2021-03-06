# org-roam-clj.markdown



??? tip  "(`ns`)"

    ```clojure
    (ns org-roam-clj.markdown
      (:require
       [clojure.core.async :as a :refer (chan)]
       [clojure.java.io :as io]
       [clojure.java.shell :as sh]
       [clojure.string :as str]
       [org-roam-clj.db]
       [org-roam-clj.async :refer (master-coordinator log-tasks worker-chan finished-tasks)]
       [org-roam-clj.utils :refer (sha1-str now-formatted replace-extension)]))
    ```

## `pandoc-cli-args`

```clojure
(pandoc-cli-args m)
```

??? tip  "(`defn`)"

    ```clojure
    (defn pandoc-cli-args [m]
      (let [key->str
            (fn [k]
              (if (keyword k)
                (let [s (name k) dash (case (count s) 1 "-" "--")]
                  (str dash s))
                k))
            value->str #(if (keyword %) (name %) %)]
        (reduce #(conj %1 (key->str (first %2)) (value->str (second %2))) [] m)))
    ```

## `pandoc`

```clojure
(pandoc file & [{:as opts}])
```

??? tip  "(`defn`)"

    ```clojure
    (defn pandoc [file & [{:as opts}]]
      (let [args (into ["pandoc" file] (pandoc-cli-args opts))
            p (ProcessBuilder. args)]
        (.start p)))
    ```

## `orgmk`

```clojure
(orgmk file)
```

??? tip  "(`defn`)"

    ```clojure
    (defn orgmk [file] (-> (ProcessBuilder. ["org2gfm" file]) (.start)))
    ```

## `work`









??? tip  "(`defmulti`)"

    ```clojure
    (defmulti work (fn [k & _] k))
    (defmethod work :default [_] identity)
    (defmethod work :pandoc [_ file]
      (let [filename (.getPath file)
            md-filename (-> (replace-extension filename "md")
                            (str/replace  #"^\./" ""))
            p (pandoc filename
                        {:t :gfm
                         :o (->> (str "docs-md/" md-filename))})
            status (.waitFor p)]
        (println (now-formatted) "Exit status for pandoc (" filename "): " status)))
    ```

### work :orgmk

```clojure
(work _ file)
```

??? info  "(`defmethod`)"

    ```clojure
    (defmethod work :orgmk [_ file]
      (let [filename (.getPath file)
            md-filename (-> (replace-extension filename "md")
                        (str/replace  #"^\./" ""))
            dst (io/file (str "docs-md/" md-filename))
            p (orgmk filename)
            status (.waitFor p)]
        (println (now-formatted) "Exit status for orgmk (" filename "): " status)
        (when (zero? status)
          (when-not (.exists dst)
            (io/make-parents dst))
          (io/copy (io/file md-filename) dst)
          (io/delete-file md-filename))))
    ```
### work :mkdocs

```clojure
(work _ c)
```

??? info  "(`defmethod`)"

    ```clojure
    (defmethod work :mkdocs [_ c]
      (a/put! c (sh/sh "mkdocs" "build")))
    ```
## `worker-pool`

```clojure
(worker-pool n log-chan)
```

??? tip  "(`defn`)"

    ```clojure
    (defn worker-pool [n log-chan]
      (mapv (fn [_] (worker-chan work log-chan)) (range n)))
    ```

## `modified-files`

```clojure
(modified-files fs)
```

??? tip  "(`defn`)"

    ```clojure
    (defn modified-files [fs]
      (let [files-hashes (->> (org-roam-clj.db/files-hashes)
                              (reduce #(assoc %1 (:file %2) (:hash %2)) {}))]
        (filter #(not= (get files-hashes (str (.getCanonicalPath %)))
                       (sha1-str (slurp (str %))))
                fs)))
    ```

## `convert-org-files`

```clojure
(convert-org-files)
(convert-org-files root)
```

??? tip  "(`defn`)"

    ```clojure
    (defn convert-org-files
      ([] (convert-org-files "."))
      ([root]
       (let [fs (->> (file-seq (io/file root))
                     (filter #(str/ends-with? % ".org"))
                     modified-files)
             size 16
             master-chan (chan size)
             log-chan (chan size)
             log-mult (a/mult log-chan)
             println-chan (chan size)
             task-chan (chan size)
             terminated-tasks (finished-tasks task-chan)
             elapsed-time (atom 0)]
         (a/tap log-mult println-chan)
         (a/tap log-mult task-chan)
         (log-tasks println-chan)
         (master-coordinator master-chan log-chan (worker-pool size log-chan))
         (doseq [org-file fs]
           (a/>!! master-chan [:orgmk org-file]))
         (while (and (not= (count @terminated-tasks) (count fs))
                     (not= @elapsed-time 3600))
           (when-not (mod @elapsed-time 5)
             (a/offer! println-chan [:log "Waiting for conversion to finish"])
             (a/offer! println-chan [:log "Ratio: " (count @terminated-tasks) "/" (count fs)]))
           (a/<!! (a/timeout 1000))
           (swap! elapsed-time inc))
         (println (count @terminated-tasks) (count fs))
         (let [final-chan (chan)]
           (a/>!! master-chan [:mkdocs final-chan])
           (println (a/<!! final-chan)))
         (println "Store files hashes for efficiency")
         (org-roam-clj.db/create-files-clj)
         (doseq [f fs]
           (org-roam-clj.db/store-hash-content! f)))))
    ```

## `clean-folder`

```clojure
(clean-folder src)
```

??? tip  "(`defn`)"

    ```clojure
    (defn clean-folder [src]
      (let [fs (filter #(str/ends-with? % ".md") (file-seq (io/file src)))]
        (doseq [f fs]
          (io/delete-file f))))
    ```

## `-main`

```clojure
(-main)
```

??? tip  "(`defn`)"

    ```clojure
    (defn -main []
      (println "Starting to convert org-files")
      (convert-org-files)
      (println "End of script"))
    ```

## Rich Comment

```clojure
(comment
  (tap> (org-roam-clj.db/files-hashes))

)
```

