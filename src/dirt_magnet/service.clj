(ns dirt-magnet.service
  (:require [io.pedestal.service.http :as bootstrap]
            [io.pedestal.service.http.route :as route]
            [io.pedestal.service.http.body-params :as body-params]
            [io.pedestal.service.http.route.definition :refer [defroutes]]
            [io.pedestal.service.http.ring-middlewares :as middlewares]
            [ring.util.response :as ring-resp]
            [dirt-magnet.links :as links]
            [dirt-magnet.templates :as templates]
            [dirt-magnet.config :as c]))


(defn tmpl [template & data]
  (apply str (apply template data)))

(declare url-for)
(defn get-index-page
  [request]
  (let [page  (or (some-> request :query-params :p Integer.) 1)
        links (links/get-links (- page 1))]
    (tmpl templates/index links page url-for)))

(defn index-page [request]
  (ring-resp/response (get-index-page request)))

(defn create-link [{{:keys [source url]} :params :as request}]
  "Pass request to user-supplied acceptance fn, referring responses to user accepted/denied fns."
  (if (c/link-acceptable? request)
    (-> {:source source :url url}
        links/store-link
        (c/link-accepted request))
    (c/link-rejected request)))

(defroutes routes
  [[["/" {:get [::index-page index-page]}
     ^:interceptors [body-params/body-params
                     middlewares/keyword-params
                     bootstrap/html-body]
     ["/links" {:post create-link}]]]])

(def url-for (route/url-for-routes routes))

(def service {:env :prod
              ::bootstrap/routes routes
              ::bootstrap/resource-path "/public"
              ::bootstrap/type :jetty
              ::bootstrap/port 8080})
