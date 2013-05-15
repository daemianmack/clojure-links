(ns bot-splatter.service
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [io.pedestal.service.http :as bootstrap]
            [io.pedestal.service.http.route :as route]
            [io.pedestal.service.http.body-params :as body-params]
            [io.pedestal.service.http.route.definition :refer [defroutes]]
            [io.pedestal.service.http.ring-middlewares :as middlewares]
            [ring.middleware.session.cookie :as cookie]
            [ring.util.response :as ring-resp]
            [bot-splatter.links :as links]
            [bot-splatter.pages :as pages]
            [bot-splatter.templates :as templates]))

(defn layout*
  [path {:keys [flash]}]
  (when-let [res (-> (str "bot-splatter/public/design/" path) io/resource)]
    (-> (slurp res)
        (templates/layout :flash flash)
        string/join
        ring-resp/response
        (ring-resp/content-type "text/html"))))

(defn layout
  "Returns a handler that renders content from the design
   directory, with flash, wrapped in layout."
  [path]
  (fn [request]
     (layout* path request)))

(defn about-page
  [request]
  (ring-resp/response (format "Clojure %s" (clojure-version))))

(defn get-index-page
  [request]
  (let [page   (or (some-> request :query-params :p Integer.) 1)
        links  (links/get-links (- page 1))
        body   (apply str (pages/index links))
        footer (apply str (pages/footer page))]
    (apply str (templates/layout body footer))))

(defn index-page [request]
  (-> (get-index-page request)
      ring-resp/response
      (ring-resp/content-type "text/html")))

(defn create-link [{:keys [params]}]
  (links/store-link params)
  (ring-resp/response params))

(defroutes routes
  [[["/" {:get index-page}
     ^:interceptors [(body-params/body-params)
                     middlewares/keyword-params
                     (middlewares/session
                      {:store (cookie/cookie-store
                               {:key "asw23tasd9y4nbas"})})
                     middlewares/flash
                     bootstrap/html-body
                     middlewares/file-info]
     ["/links" {:post create-link}]]]])

;; You can use this fn or a per-request fn via io.pedestal.service.http.route/url-for
(def url-for (route/url-for-routes routes))

;; Consumed by bot-splatter.server/create-server
(def service {:env :prod
              ;; You can bring your own non-default interceptors. Make
              ;; sure you include routing and set it up right for
              ;; dev-mode. If you do, many other keys for configuring
              ;; default interceptors will be ignored.
              ;; :bootstrap/interceptors []
              ::bootstrap/routes routes

              ;; Uncomment next line to enable CORS support, add
              ;; string(s) specifying scheme, host and port for
              ;; allowed source(s):
              ;;
              ;; "http://localhost:8080"
              ;;
              ;;::boostrap/allowed-origins ["scheme://host:port"]

              ;; Root for resource interceptor that is available by default.
              ::bootstrap/resource-path "/public"

              ;; Either :jetty or :tomcat (see comments in project.clj
              ;; to enable Tomcat)
              ;;::bootstrap/host "localhost"
              ::bootstrap/type :jetty
              ::bootstrap/port 8080})
