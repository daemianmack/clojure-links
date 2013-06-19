(ns dirt-magnet.config
  (:require [clojure.java.jdbc :as jdbc]
            [dirt-magnet.storage :as storage]
            [ring.util.response :refer [response]]))


(def title "#clojure")

(def debug true)

(def links-per-page 40)

(def http-params
  "Used for fetching content-type and bodies for URLs."
  {:socket-timeout 10000 :conn-timeout 10000 :max-redirects 5})

(def keep-last
  "Periodically prune link DB table to hold no more than `keep-last` items."
  10000)

(defn log [& msg]
  (when debug (apply println (concat msg "\n"))))


(defn link-acceptable? [{:keys [params]}]
  (log "link-acceptable? is considering a POST with params" params)
  (= "clojure" (:password params)))

(defn input-gate []
  "Delete next-to-last link so we don't get swamped with spam."
  (jdbc/db-do-commands (storage/with-conn) false "delete from links where ctid = any (array (select ctid from links order by created_at desc offset 1 limit 1))"))

(defn link-accepted [creation-result request]
  (log "+++ Accepted a POST for params" (:params request))
  (input-gate)
  (storage/back-delete :links keep-last)
  (response {:status-code 201
             :message creation-result}))

(defn link-rejected [request]
  (log "--- Rejected a POST for request" request)
  (response {:status-code 400
             :message "Nope."}))
