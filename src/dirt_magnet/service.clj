(ns dirt-magnet.service
  (:require [io.pedestal.service.http :as bootstrap]
            [io.pedestal.service.http.route :as route]
            [io.pedestal.service.http.body-params :as body-params]
            [io.pedestal.service.http.route.definition :refer [defroutes]]
            [io.pedestal.service.http.ring-middlewares :as middlewares]
            [io.pedestal.service.http.sse :as sse]
            [io.pedestal.service.log :as log]
            [ring.util.response :as ring-resp]
            [dirt-magnet.links :as links]
            [dirt-magnet.templates :as templates]
            [dirt-magnet.config :as c]
            [dirt-magnet.subscriptions :as subs]))

(defn tmpl [template & data]
  (apply str (apply template data)))

(declare url-for)
(defn get-index-page
  [request]
  (let [page  (or (some-> request :query-params :p Integer.) 1)
        links (links/get-links (- page 1))
        next  (links/page-exists? page)]
    (tmpl templates/index links page next url-for)))

(defn index-page [request]
  (ring-resp/response (get-index-page request)))

(defn create-link [{{:keys [source url]} :params :as request}]
  "Pass request to user-supplied acceptance fn, referring responses to user accepted/denied fns."
  (if (c/link-acceptable? request)
    (let [[{:keys [id] :as result}] (links/insert-link {:source source :url url})]
      (future (-> id links/fetch-title-if-html templates/str-row subs/send-to-subscribers))
      (c/link-accepted result request))
    (c/link-rejected request)))

(defn register-user-for-updates
  "Saves context for SSE streaming."
  [context]
  (if-let [id (get-in context [:request :query-params :id])]
    (subs/add-to-subscribers id context)
    (log/error :msg "No id passed to /contributions. Ignored.")))

(defroutes routes
  [[["/" {:get [::index-page index-page]}
     ^:interceptors [body-params/body-params
                     middlewares/keyword-params
                     bootstrap/html-body]
     ["/links"   {:post create-link}]
     ["/updates" {:get [::register (sse/start-event-stream register-user-for-updates)]}]]]])

(def url-for (route/url-for-routes routes))

(def service {:env :prod
              ::bootstrap/routes routes
              ::bootstrap/resource-path "/public"
              ::bootstrap/type :jetty
              ::bootstrap/port 8080})
