(ns dirt-magnet.templates
  (:use net.cgrand.enlive-html))

(deftemplate layout "public/design/layout.html"
  [header body footer & {[level msg :as flash] :flash}]
  [:#header] (html-content header)
  [:#main]   (html-content body)
  [:#footer] (html-content footer)
  [:#flash]  (do-> (append msg) (set-attr :class (if flash
                                                   (str "alert alert-" (name level))
                                                   "hidden"))))
