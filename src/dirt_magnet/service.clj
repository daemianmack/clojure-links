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
            [dirt-magnet.subscriptions :as subs]
            [dirt-magnet.content-types :as con-neg]))

(defn tmpl [template & data]
  (apply str (apply template data)))

(defn get-page-param [req]
  (or (some-> req :query-params :p Integer.) 1))

(declare url-for)
(defn get-page-links
  [request]
  (let [page  (get-page-param request)
        links (links/get-links (- page 1))]
    links))

(defn handle-html-request [links page-param]
  (let [next-page? (links/page-exists? page-param)
        page-route (url-for ::index-page)]
    (tmpl templates/index links page-param next-page? page-route)))

(defn index-page [request]
  (let [links (get-page-links request)]
    (if (= "text/html" (-> request :pedestal.content-negotiation/content-negotiation :content-type))
      (ring-resp/response (handle-html-request links (get-page-param request)))
      (ring-resp/response links))))

(defn keyword-all-params [request]
  (let [params (or (:edn-params request)
                   (:json-params request)
                   (:params request))
        keyword-fn (fn [result [k v]] (conj result {(keyword k) v}))]
    (reduce keyword-fn {} params)))

(defn fetch-link-title-later [{id :id}]
  (future (-> id links/fetch-title-if-html templates/str-row subs/send-to-subscribers)))

(defn create-link [request]
  "Pass request to user-supplied acceptance fn, referring responses to user accepted/denied fns."
  (let [params (keyword-all-params request)
        [source url] ((juxt :source :url) params)
        request (assoc request :params params)]
    (if-not (c/link-acceptable? request)
      (c/link-rejected request)
      (let [result (links/insert-link source url)]
        (fetch-link-title-later (first result))
        (c/link-accepted result request)))))

(defn register-user-for-updates
  "Saves context for SSE streaming."
  [context]
  (if-let [id (get-in context [:request :query-params :id])]
    (subs/add-to-subscribers id context)
    (log/error :msg "No id passed to /contributions. Ignored.")))

(defroutes routes
  [[["/" ^:interceptors [body-params/body-params]
     ["/" {:get [::index-page index-page]}
      ^:interceptors [con-neg/html+edn+json-interceptor]]
     ["/links"   {:post create-link}
      ^:interceptors [con-neg/html+edn+json-interceptor]]
     ["/updates" {:get [::register (sse/start-event-stream register-user-for-updates)]}]]]])

(def url-for (route/url-for-routes routes))

(def service {:env :prod
              ::bootstrap/routes routes
              ::bootstrap/resource-path "/public"
              ::bootstrap/type :jetty
              ::bootstrap/port 8080})
