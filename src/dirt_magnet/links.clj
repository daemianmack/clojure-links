(ns dirt-magnet.links
  (:require (clj-time [format :as f]
                      [coerce :as c]
                      [local :refer [local-now]])
            [io.pedestal.service.http.sse :as sse]
            [dirt-magnet.storage :as s]
            [dirt-magnet.config :as config]
            [clj-http.client :as client])
  (:use [net.cgrand.enlive-html]))

(defn get-body [url]
  "Return body of URL. If failure, return nil."
  (try
    ((client/get url config/http-params) :body)
    (catch Exception e)))

(defn get-title [url]
  "Return the scraped title of the URL for good URLs, or the URL itself for failed URL fetches."
  (when-let [body (get-body url)]
    (second (re-find #"<title>(.*?)</title>" (.replaceAll body "[\t\n\r]+" " ")))))

(defn is-html? [url]
  (not (nil? (re-find #"text/html"
                      (-> url client/head :headers (#(%1 "content-type")))))))

(defn is-image? [url]
  "Force boolean response to regex searches."
  (not (nil? (re-find #"\.(jpg|jpeg|gif|png)$" url))))

(declare get-link)
(defn fetch-title-if-html [id]
  "Return the link, preferably after updating its title."
  (let [{:keys [url is_image] :as link} (get-link id)]
    (if (or is_image
            (not (is-html? url)))
      link
      (if-let [title (get-title url)]
        (do
          (s/update-table :links {:title title} {:id id})
          (assoc link :title title))
        link))))

(defn timestamp [] (c/to-timestamp (local-now)))

(defn insert-link [source url]
  (let [is_image   (is-image? url)
        created_at (timestamp)]
    (s/insert-into-table :links {:source source
                                 :url url
                                 :is_image is_image
                                 :created_at created_at})))

(defn get-link [id]
  (first (s/query (str "select * from links where id = " id))))

(defn get-links
  ([] (get-links 0))
  ([page]
     (let [offset (* page config/links-per-page)]
       (s/query (str "select * from links order by created_at desc limit " config/links-per-page " offset " offset)))))

(defn page-exists? [page]
  (< 0 (count (get-links page))))

