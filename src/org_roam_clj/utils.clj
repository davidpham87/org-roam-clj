(ns org-roam-clj.utils
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   [java.net URI]
   [java.nio.file Paths]
   [java.security MessageDigest]))

(defn debug [x]
  (println x)
  x)

(defn now [] (java.time.ZonedDateTime/now))
(defn now-formatted []
  (.format (now) (java.time.format.DateTimeFormatter/ofPattern "YYYYMMdd hhmmss.SSS: ")))

(defn extension [path] (subs path (str/last-index-of path ".")))

(defn replace-extension [path extension]
  (-> path
      (subs 0 (str/last-index-of path "."))
      (str "." extension)))

(defn replace-root [path root]
  (str root "/" (str/replace path #"^[^/]*/" "")))

(defn ->Path [s]
  (->> (str/replace s #"^\." "")
       (str "file://")
       (URI.)
       Paths/get
       (.normalize)))

(defn ->relative-path
  "Returns the relative path of `location` with respect to `root`"
  [root location]
  (-> (.relativize (->Path root) (->Path location))
      (.normalize)
      str))

(defn gather-org-files
  ([] (gather-org-files "."))
  ([root]
   (into [] (comp (filter #(str/ends-with? % ".org"))
                  (map #(.getCanonicalPath %)))
         (file-seq (io/file root)))))

(defn sha1-str [s]
  (let [digested (-> "sha1"
                     MessageDigest/getInstance
                     (.digest (.getBytes s)))]
    (->> digested
         (map #(.substring
                (Integer/toString
                 (+ (bit-and % 0xff) 0x100) 16) 1))
         (apply str))))

(defn gather-org-files [root]
  (filterv #(str/ends-with? % ".org") (file-seq (io/file root))))
