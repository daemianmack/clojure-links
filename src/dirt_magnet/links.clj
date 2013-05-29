(ns dirt-magnet.links
  (:require (clj-time [format :as f]
                      [coerce :as c]
                      [local :refer [local-now]])
            [dirt-magnet.storage :as s]
            [dirt-magnet.config :as config]
            [clj-http.client :as client])
  (:use [net.cgrand.enlive-html]))


(defn get-body [url]
  "Return body of URL. If failure, return URL."
  (try
    ((client/get url config/http-params) :body)
    (catch Exception e
      url)))

(defn get-title [url]
  "Return the scraped title of the URL for good URLs, or the URL itself for failed URL fetches."
  (when-let [body (get-body url)]
    (if (= body url)
      url
      (second (re-find #"<title>(.*?)</title>" (.replaceAll body "[\t\n\r]+" " "))))))

(defn is-html? [url]
  (re-find #"text/html"
           (-> url client/get :headers (#(%1 "content-type")))))

(defn is-image? [url]
  "Force boolean response to regex searches."
  (not (nil? (re-find #"\.(jpg|jpeg|gif|png)$" url))))

(defn fetch-title-if-html [id url is-image then]
  (when (and (not is-image)
             (is-html? url))
    (when-let [title (get-title url)]
      (s/update-table :links {:title title} {:id id}))))

(defn now->nice-format []
  "Get local now suitable for passing to nice-format->timestamp."
  (f/unparse config/nice-format (local-now)))

(defn nice-format->timestamp
  "Accept input in nice-format, transmute to SQL-storable format."
  [date] (->> date
              (f/parse config/nice-format)
              c/to-timestamp))

(defn insert-link
  [{:keys [id title source url is_image created_at] :as data}]
  (let [data (assoc data :created_at (if created_at
                                       (nice-format->timestamp created_at)
                                       (nice-format->timestamp (now->nice-format))))
        data (if id data (dissoc data :id))
        data (assoc data :is_image (is-image? url))]
    (s/insert-into-table :links data)))

(defn store-link [data]
  (let [[{:keys [id url is_image] :as result}] (insert-link data)]
    (future (fetch-title-if-html id url is_image (local-now)))
    result))

(defn get-links
  ([] (get-links 0))
  ([page]
     (let [offset (* page config/links-per-page)]
       (s/query (str "select * from links order by created_at desc limit " config/links-per-page " offset " offset)))))
