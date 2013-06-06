(ns dirt-magnet.templates
  (:require [dirt-magnet.config :as config])
  (:use net.cgrand.enlive-html))


(defn do-link-title [link]
  (apply str (take 80 (or (:title link) (:url link)))))

(defn do-image-with-title [link]
  (str "<img src='" (:url link) "'>"))

(defn do-link [link]
  (let [title (if (:is_image link)
                (do-image-with-title link)
                (do-link-title link))]
    (do-> (set-attr :href (:url link))
          (html-content title))))

(defsnippet row "public/design/main.html"
  [[:tr (nth-of-type 1)]]
  [link]
  [[:td (nth-of-type 1)]]    (do-> (set-attr :title (:created_at link))
                                   (content (str (:source link) ">")))
  [[:td (nth-of-type 2)] :a] (do-link link))

(deftemplate index "public/design/layout.html"
  [links page next-page url-fn]
  [:#header :a] (do-> (set-attr :href (url-fn ::dirt-magnet.service/index-page))
  [:#links :tbody] (content (map row links))
                      (content config/title))
  [:#footer :a] (when next-page
                  (set-attr :href (str (url-fn ::dirt-magnet.service/index-page) "?p=" (+ 1 page))))
)

(defn str-row [body]
  (apply str (emit* (row body))))
