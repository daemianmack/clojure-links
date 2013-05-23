(ns dirt-magnet.config
  (:require (clj-time [format :as f])))



(def debug true)

(def links-per-page 40)

(def nice-format (f/formatter "yyyy-MM-dd HH:mm:ss"))


(defn log [& msg]
  (when debug (apply println (concat msg "\n"))))


(defn link-acceptable? [{:keys [params]}]
  (log "link-acceptable? is considering a POST with params" params)
  (= "professor-falken" (:password params)))

(defn link-accepted [creation-result request]
  creation-result)
  (log "+++ Accepted a POST for params" (:params request))

(defn link-rejected [request]
  "Nope!")
  (log "--- Rejected a POST for request" request)
