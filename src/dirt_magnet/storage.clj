(ns dirt-magnet.storage
  (:require [clojure.java.jdbc :as j]
            [clojure.java.jdbc.sql :refer [where]]
            [io.pedestal.service.log :as log]))


(defn db-map []
  "bash> export DATABASE_URL=postgres://user:password@host:port/database"
  (if (nil? (System/getenv "DATABASE_URL"))
    (do
      (log/error :error "Missing DATABASE_URL environment variable. Abandon all hope.")
      (throw (Exception. "Missing DATABASE_URL environment variable. Abandon all hope.")))
    (let [[_ user password host port database] (re-matches #"postgres://(?:(.+):(.*)@)?([^:]+)(?::(\d+))?/(.+)" (System/getenv "DATABASE_URL"))]
      {:subprotocol "postgresql"
       :subname     (str "//" host ":" port "/" database)
       :classname   "org.postgresql.Driver"
       :user        user
       :password    password})))

(defn mk-conn [] (assoc (db-map) :connection (j/get-connection (db-map))))

(def conn nil)

(defn with-conn []
  (when (nil? conn)
    (alter-var-root #'conn (constantly (mk-conn))))
  conn)

(def db-schema [:links
                [:id          :serial]
                [:title       :text]
                [:source      :text]
                [:url         :text]
                [:is_image    :boolean]
                [:created_at  "timestamp with time zone"]])

(defn apply-schema []
  (try
    (j/db-do-commands (with-conn) false (apply j/create-table-ddl db-schema))
    (j/db-do-commands (with-conn) false "CREATE UNIQUE INDEX link_id ON links (id);")
    (catch Exception e
      (println e))))

(defn insert-into-table [table data]
  (j/insert! (with-conn) table data))

(defn update-table [table data where-data]
  (j/update! (with-conn) table data (where where-data)))

(defn query [q]
  (j/query (with-conn) [q]))

