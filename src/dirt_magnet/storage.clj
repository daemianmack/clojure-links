(ns dirt-magnet.storage
  (:require [clojure.java.jdbc :as j])
  (:require [clojure.java.io :as io]))


(defn db-map []
  (let [[_ user password host port database] (re-matches #"postgres://(?:(.+):(.*)@)?([^:]+)(?::(\d+))?/(.+)" (System/getenv "DATABASE_URL"))]
    {:subprotocol "postgresql"
     :subname     (str "//" host ":" port "/" database)
     :classname   "org.postgresql.Driver"
     :user        user
     :password    password}))

(def db (assoc (db-map) :connection (j/get-connection (db-map))))

(def db-schema [:links
                [:id          :serial]
                [:title       :text]
                [:source      :text]
                [:url         :text]
                [:is_image    :boolean]
                [:created_at  "timestamp with time zone"]])

(defn apply-schema []
  (try
    (j/db-do-commands db false (apply j/create-table-ddl db-schema))
    (j/db-do-commands db false "CREATE UNIQUE INDEX link_id ON links (id);")
    (catch Exception e
      (println e)
      (println (.printStackTrace (.getCause e))))))

(defn insert-into-table [[table data]]
  (try
    (j/insert! db table data)
    (catch Exception e
      (println e)
      (println (.getNextException e)))))

(defn query [q]
  (try
    (j/query db [q])
    (catch Exception e
      (println e))))

