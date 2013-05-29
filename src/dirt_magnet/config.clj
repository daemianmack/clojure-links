(ns dirt-magnet.config
  (:require (clj-time [format :as f])
            [ring.util.response :refer [response]]))



(def title "dirt magnet")

(def debug true)

(def links-per-page 40)

(def nice-format (f/formatter "yyyy-MM-dd HH:mm:ss"))

(def http-params
  "Used for fetching content-type and bodies for URLs."
  {:socket-timeout 10000 :conn-timeout 10000 :max-redirects 5})


(defn log [& msg]
  (when debug (apply println (concat msg "\n"))))


(defn link-acceptable? [{:keys [params]}]
  (log "link-acceptable? is considering a POST with params" params)
  (= "professor-falken" (:password params)))

(defn link-accepted [creation-result request]
  (log "+++ Accepted a POST for params" (:params request))
  (response creation-result))

(defn link-rejected [request]
  (log "--- Rejected a POST for request" request)
  (response {:status "Nope."}))
