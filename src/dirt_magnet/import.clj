(ns dirt-magnet.import
  (:require [dirt-magnet.links :as links]
            [clojure.string :refer [split]]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as j]
            [clojure.java.jdbc.sql :refer [where]]
            (clj-time [coerce :as c]
                      [format :as f]
                      [local :refer [local-now]])
            [dirt-magnet.storage :as s]))


(defn correct-sequence []
  (let [[{:keys [max]}] (j/query (s/with-conn) ["select max(id) from links"])]
    (j/query (s/with-conn) [(str "select setval('links_id_seq', " max ")")])))

(defn timestamp [date]
    (c/to-timestamp (f/parse (f/formatter "yyyy-MM-dd HH:mm:ss") date)))

(def int->bool {"0" false "1" true "\\N" false})

(defn make-mysql-link-importable [id title source url is_image created_at]
  (let [id          (Integer. id)
        is_image    (int->bool is_image)
        title       (if (= title "") nil title)
        created_at  (timestamp created_at)]
    {:id id
     :title title
     :source source
     :url url
     :is_image is_image
     :created_at created_at}))

(defn import-tab-delimited-dump [file]
  "Import from similar app in tab-delimited format. Assume all fields
   we store are already populated."
  (with-open [rdr (io/reader (io/resource file))]
    (doseq [line (line-seq rdr)]
      (let [[id title source url _ _ created_at _ is_image] (split line #"\t")]
        (s/insert-into-table :links
                             (make-mysql-link-importable id title source url is_image created_at))
        (correct-sequence)))))

(defn import-weechat-log [log]
  "Import from weechat log format.

   2013-05-01 13:46:17^ILORDKAHUNA^Ihttp://mashable.com/2013/04/24/william-gibson-google-glass/
   2013-05-01 13:46:30^ILORDKAHUNA^I'Was faintly annoyed at just how interesting I found the experience.'
   =>
   {:url http://mashable.com/2013/04/24/william-gibson-google-glass/
    :source LORDKAHUNA
    :created_at #inst 2013-05-01T13:46:17.000000000-00:00}

   After insert, the URL's title will be fetched and updated asynchronously."
  (with-open [rdr (io/reader (io/resource log))]
    (doseq [line (line-seq rdr)]
      (when (and (re-find #"https?://[^\s]+" line)
                 (and (not (re-find #"Topic for" line))
                      (not (re-find #"URL for" line))))
        (let [[created_at source url] (split line #"\t")
              url (re-find #"https?://[^\s]+" url)]
          (let [[{:keys [id]}] (links/insert-link source url)]
            (future (links/fetch-title-if-html id)))))))
  (correct-sequence))

(defn import-clojure-logdir [dir]
  "Enumerate all files beneath the given dir, find lines containing a URL,
   and parse the particular IRC log format thereof to persist url, source, and
   created_at. Filenames are assumed to contain the date, e.g. 2013-01-28.txt.

  [18:24:09] Raynes: Ahhhhhh! XML!
  [18:24:26] *Raynes http://tf2chan.net/dis/src/129470155772.jpg
  [18:24:50] st3fan: yeah :-)
  =>
  {:url http://tf2chan.net/dis/src/129470155772.jpg
   :source Raynes
   :is_image true
   :created_at #inst 2013-01-28T18:24:26.000000000-00:00}

   More work could be done here to clean leading/trailing punctuation out of
   URLs referenced in the middle of a sentence."
  (doseq [file (-> dir io/resource io/file file-seq rest)]
    (with-open [rdr (io/reader file)]
      (doseq [line (line-seq rdr)]
        (when (re-find #"https?://[^\s]+" line)
          (let [[brackets-time source-colon line] (split line #"\s" 3)
                yyyy-dd-mm (-> file .getName (subs 0 10))
                created_at (str yyyy-dd-mm " " (apply str (butlast (rest brackets-time))))
                source     (apply str (butlast source-colon))
                url        (re-find #"https?://[^\s]+" line)]
            (let [[{:keys [id]}] (s/insert-into-table :links
                                                      {:url url
                                                       :source source
                                                       :is_image (links/is-image? url)
                                                       :created_at (timestamp created_at)})]
              (future (links/fetch-title-if-html id))))))))
  (correct-sequence))
