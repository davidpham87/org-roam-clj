(ns user
  (:require
   [clojure.core.async :as a :refer (<! >! go go-loop chan)]
   [clojure.java.io :as io]
   [clojure.java.shell :as sh]
    [clojure.string :as str]
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as rs]
   [orgmode.core])
  (:import
   [java.net URI]
   [java.nio.file Paths]))

(def db
  {:dbtype "sqlite"
   :dbname "org-roam.db"})

(defn data-resource [] (jdbc/get-datasource db))

(def root-location (System/getProperty "user.dir"))

(def x (orgmode.core/parse "todo.org"))

(defn get-tags [x]
  (str/split (get-in x [:attribs :tags] "")  #"\s+"))

(defn ->Path [s] (Paths/get (URI. (str "file://" s))))

(defn ->relative-path
  "Returns the relative path of `location` with respect to `root`"
  [root location]
  (-> (.relativize (->Path root) (->Path location)) str))

(comment

  (let [x (->Path root-location)
        y (Paths/get (URI. "file:///home/david/Documents/org_files/cards/clojure.org"))]
    (str (.relativize y x)))
  (->relative-path root-location "/home/david/Documents/cards/clojure.org")

)
