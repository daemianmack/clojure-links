(ns dirt-magnet.pages
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

(defsnippet table-row "dirt-magnet/public/design/index.html"
  [:#links :tbody [[:tr (nth-of-type 1)]]]
  [link]
  [[:td (nth-of-type 1)]]    (do-> (set-attr :title (:created_at link))
                                   (content (str (:source link) ">")))
  [[:td (nth-of-type 2)] :a] (do-link link))

(deftemplate index "dirt-magnet/public/design/index.html"
  [links]
  [:#links :tbody] (content (map table-row links)))

(deftemplate footer "dirt-magnet/public/design/footer.html"
  [page]
  [:a] (set-attr :href (str "/?p=" (+ 1 page))))
