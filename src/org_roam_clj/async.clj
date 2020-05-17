(ns org-roam-clj.async
  "Some functionalities for handling with core.async."
  (:require
   [org-roam-clj.utils :refer (now-formatted)]
   [clojure.core.async :as a :refer (<! >! go-loop chan)]))

(defn log-tasks
  "Logs to console the events."
  ([] (log-tasks (chan 42)))
  ([c]
   (a/thread
     (loop []
       (when-let [s (a/<!! c)]
         (println (now-formatted) s)
         (recur)))
     (println "Closed log chan"))
   c))

(defn finished-tasks
  "Channel to collect the finished task."
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

(defn master-coordinator [c log-chan pool]
  (let [alts-pool-vec (fn [pool value] (mapv #(vector %1 %2) pool (repeat value)))]
    (go-loop []
      (when-let [x (<! c)]
        (>! log-chan [:received x])
        (a/alts! (alts-pool-vec pool x))
        (recur))))
  c)

(defn emit-end [f c]
  (fn [& args]
    (apply f args)
    (a/offer! c (vec args))))

(defn worker-chan
  "Create a worker channel for handling async working. TODO: maybe add the
  options to create thread instead of go-loop."
  [f log-chan]
  (let [c (chan)]
    (go-loop []
      (when-let [[k & args] (<! c)]
        (>! log-chan [:start k args])
        (apply f k args)
        (>! log-chan [:end k args])
        (recur)))
    c))
