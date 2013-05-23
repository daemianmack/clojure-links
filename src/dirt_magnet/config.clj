(ns dirt-magnet.config
  (:require (clj-time [format :as f])))



(def debug true)

(def links-per-page 40)

(def nice-format (f/formatter "yyyy-MM-dd HH:mm:ss"))


(defn log [msg]
  (when debug (println msg)))

(defn link-acceptable? [{:keys [params]}]
  (println "link-acceptable? is considering a POST with params " params)
  (= "professor-falken" (:password params)))

(defn link-accepted [creation-result request]
  (log "+++ Accepted a POST for params " (:params request))
  creation-result)

(defn link-rejected [request]
  (log "--- Rejected a POST for request " request)
  "Nope!")
