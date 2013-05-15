(ns dirt-magnet.import
  (:require [dirt-magnet.links :as links]
            [clojure.string :refer [split]]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as j]
            [clojure.java.jdbc.sql :refer [where]]
            (clj-time [local :refer [local-now]])
            [dirt-magnet.storage :as s]))


(defn correct-sequence []
  (let [[{:keys [max]}] (j/query s/db ["select max(id) from links"])]
    (j/query s/db [(str "select setval('links_id_seq', " max ")")])))

(def boolint {"0" false "1" true "\\N" false})

(defn make-mysql-link-importable [id title source url is_image created_at]
  (let [id          (Integer. id)
        is_image    (boolint is_image)]
    (links/store-link
     {:id id :title title :source source
      :url url :is_image is_image :created_at created_at})))

(defn fix-data []
  (j/update! s/db :links {:title nil} (where {:title ""})))

(defn import-mysql-dump [file]
  (with-open [rdr (io/reader (io/resource file))]
    (doseq [line (line-seq rdr)]
      (let [[id title source url _ _ created_at _ is_image] (split line #"~~~")]
        (make-mysql-link-importable id title source url is_image created_at))))
  (fix-data)
  (correct-sequence))

(defn import-log [log]
  (with-open [rdr (io/reader (io/resource log))]
    (doseq [line (line-seq rdr)]
      (when (and (re-find #"https?://[^\s]+" line)
                 (and (not (re-find #"Topic for" line))
                      (not (re-find #"URL for" line))))
        (let [[created_at source url] (split line #"\t")
              url (re-find #"https?://[^\s]+" url)]
          (links/store-link {:url url :source source :created_at created_at})))))
  (correct-sequence))

(defn do-full-import []
  (import-mysql-dump "links.txt")
  (import-log "irc_log.txt"))



;; bulk-resolve link titles
(defn bail [id msg]
  (spit "foo.txt" (str (local-now) "xx " id ": " msg "\n") :append true))

(defn record [id title]
  (j/update! s/db :links {:title title} (where {:id id}))
  (spit "foo.txt" (str (local-now) "++ " id ": " title "\n") :append true))

;; (defn unresolved []
;;   (j/query s/db ["select id, url from links where is_resolved != '1' and is_image != '1' limit 10"]))

(defn resolve-unresolved-titles []
  (map (fn [{:keys [id url]}]
         (future (try
                   (record id (links/get-title url))
                   (catch Exception e (bail id e)))))
       (unresolved)))
