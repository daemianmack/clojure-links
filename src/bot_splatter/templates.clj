(ns bot-splatter.templates
  (:use net.cgrand.enlive-html))

(deftemplate layout "bot-splatter/public/design/layout.html"
  [body footer & {[level msg :as flash] :flash}]
  [:#main]   (html-content body)
  [:#footer] (html-content footer)
  [:#flash]  (do-> (append msg) (set-attr :class (if flash
                                                   (str "alert alert-" (name level))
                                                   "hidden"))))
