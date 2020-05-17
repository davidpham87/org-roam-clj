# org-roam-clj.async

Some functionalities for handling with core.async.



??? tip  "(`ns`)"

    ```clojure
    (ns org-roam-clj.async
      (:require
       [org-roam-clj.utils :refer (now-formatted)]
       [clojure.core.async :as a :refer (<! >! go-loop chan)]))
    ```

## `log-tasks`

Logs to console the events.

```clojure
(log-tasks)
(log-tasks c)
```

??? tip  "(`defn`)"

    ```clojure
    (defn log-tasks
      ([] (log-tasks (chan 42)))
      ([c]
       (a/thread
         (loop []
           (when-let [s (a/<!! c)]
             (println (now-formatted) s)
             (recur)))
         (println "Closed log chan"))
       c))
    ```

## `finished-tasks`

Channel to collect the finished task.

```clojure
(finished-tasks c)
```

??? tip  "(`defn`)"

    ```clojure
    (defn finished-tasks
      [c]
      (let [result (atom [])]
        (a/thread
          (loop []
            (when-let [x (a/<!! c)]
              (when (= (first x) :end)
                (swap! result conj x))
              (recur)))
          (println "Finished tasks"))
        result))
    ```

## `master-coordinator`

```clojure
(master-coordinator c log-chan pool)
```

??? tip  "(`defn`)"

    ```clojure
    (defn master-coordinator [c log-chan pool]
      (let [alts-pool-vec (fn [pool value] (mapv #(vector %1 %2) pool (repeat value)))]
        (go-loop []
          (when-let [x (<! c)]
            (>! log-chan [:received x])
            (a/alts! (alts-pool-vec pool x))
            (recur))))
      c)
    ```

## `emit-end`

```clojure
(emit-end f c)
```

??? tip  "(`defn`)"

    ```clojure
    (defn emit-end [f c]
      (fn [& args]
        (apply f args)
        (a/offer! c (vec args))))
    ```

## `worker-chan`

Create a worker channel for handling async working. TODO: maybe add the
  options to create thread instead of go-loop.

```clojure
(worker-chan f log-chan)
```

??? tip  "(`defn`)"

    ```clojure
    (defn worker-chan
      [f log-chan]
      (let [c (chan)]
        (go-loop []
          (when-let [[k & args] (<! c)]
            (>! log-chan [:start k args])
            (apply f k args)
            (>! log-chan [:end k args])
            (recur)))
        c))
    ```

