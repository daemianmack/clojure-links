(ns dirt-magnet.links
  (:require (clj-time [format :as f]
                      [coerce :as c]
                      [local :refer [local-now]])
            [dirt-magnet.storage :as s])
  (:use [net.cgrand.enlive-html]))


(def links-per-page 40)

(def nice-format (f/formatter "yyyy-MM-dd HH:mm:ss"))


(defn get-title [url]
  (second (re-find #"<title>(.*?)</title>" (.replaceAll (slurp url) "[\t\n\r]+" " "))))

(defn now->nice-format []
  "Get local now suitable for passing to nice-format->timestamp."
  (f/unparse nice-format (local-now)))

(defn nice-format->timestamp
  "Accept input in nice-format, transmute to SQL-storable format."
  ([date] (->> date
               (f/parse nice-format)
               c/to-timestamp)))

(defn is-image? [url]
  "Force boolean response to regex searches."
  (not (nil?
        (and
         (re-find #"\.(jpg|jpeg|gif|png)$" url)
         (not (re-find #"\.php$" url))))))

(defn store-link
  [{:keys [id title source url is_image created_at] :as data}]
  (let [data (assoc data :created_at (if created_at
                                       (nice-format->timestamp created_at)
                                       (nice-format->timestamp (now->nice-format))))
        data (if id data (dissoc data :id))
        data (assoc data :is_image (is-image? url))]
    (try
      (let [data (if title data (assoc data :title (get-title url)))]
        (s/insert-into-table [:links data]))
      (catch Exception e (println "url" url "bombed out with " e)))))

(defn get-links
  ([] (get-links 0))
  ([page]
     (let [offset (* page links-per-page)]
       (s/query (str "select * from links order by created_at desc limit " links-per-page " offset " offset)))))


