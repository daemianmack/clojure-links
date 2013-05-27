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

(defn import-weechat-log [log]
  (with-open [rdr (io/reader (io/resource log))]
    (doseq [line (line-seq rdr)]
      (when (and (re-find #"https?://[^\s]+" line)
                 (and (not (re-find #"Topic for" line))
                      (not (re-find #"URL for" line))))
        (let [[created_at source url] (split line #"\t")
              url (re-find #"https?://[^\s]+" url)]
          (links/store-link {:url url :source source :created_at created_at})))))
  (correct-sequence))

(defn import-clojure-log [dir]
  "Enumerate all files beneath the supplied dir, find lines containing a URL,
   and parse the particular IRC log format thereof to persist url, source, and
   created_at.
   More work could be done here to clean leading/trailing punctuation out of
   URLs referenced in the middle of a sentence."
  (doseq [file (-> dir io/resource io/file file-seq rest)]
    (with-open [rdr (io/reader file)]
      (doseq [line (line-seq rdr)]
        (when (re-find #"https?://[^\s]+" line)
          (let [[brackets-time source-colon line] (split line #"\s" 3)
                yyyyddmm   (-> file .getName (subs 0 10))
                created_at (str yyyyddmm " " (apply str (butlast (rest brackets-time))))
                source     (apply str (butlast source-colon))
                url        (re-find #"https?://[^\s]+" line)]
            (links/store-link {:url url :source source :created_at created_at}))))))
  (correct-sequence))

(defn do-full-import []
  (import-mysql-dump "links.txt")
  (import-log "irc_log.txt"))
