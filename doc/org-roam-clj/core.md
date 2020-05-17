# org-roam-clj.core



??? tip  "(`ns`)"

    ```clojure
    (ns org-roam-clj.core
      (:require
       [clojure.tools.cli :refer (parse-opts)]
       [org-roam-clj.markdown]
       [org-roam-clj.tags]))
    ```

## `cli-options`



??? tip  "(`def`)"

    ```clojure
    (def cli-options
      [["-t" "--task TASK" "Which task to perform. One of clear-tags, create-tags, markdown, clean-markdown"
        :default :markdown
        :parse-fn keyword
        :validate [#{:clear-tags :create-tags :markdown :clean-markdown}]]
       ["-h" "--help"]])
    ```

## `-main`

```clojure
(-main & args)
```

??? tip  "(`defn`)"

    ```clojure
    (defn -main [& args]
      (let [cli-args (parse-opts args cli-options)]
        (if (get-in cli-args [:options :help])
          (println (:summary cli-args))
          (case (:task cli-args)
            :create-tags (org-roam-clj.tags/create-tags)
            :clear-tags (org-roam-clj.tags/clear-tags)
            :markdown (org-roam-clj.markdown/convert-org-files)
            :clean-markdown (org-roam-clj.markdown/clean-folder "docs-md")))))
    ```

