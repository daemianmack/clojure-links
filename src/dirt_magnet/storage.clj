(ns dirt-magnet.storage
  (:require [clojure.java.jdbc :as j])
  (:require [clojure.java.io :as io]))


(let [db-host "localhost"
      db-port 5432
      db-name "lolserver"
      db-user "lolserver"
      db-password "b0c3phus"]
  (def db-map
    {:subprotocol "postgresql"
     :subname     (str "//" db-host ":" db-port "/" db-name)
     :classname   "org.postgresql.Driver"
     :user        db-user
     :password    db-password}))

(def db (assoc db-map :connection (j/get-connection db-map)))


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
      (.getNextException e))))

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
      (println (.getNextException e))
      (.getNextException e))))
