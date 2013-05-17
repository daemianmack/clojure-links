(ns dirt-magnet.config)

(defn link-acceptable? [{:keys [params]}]
  (println "link-acceptable? is considering a POST with params " params)
  (= "professor-falken" (:password params)))

(defn link-accepted [creation-result request]
  (println "+++ Accepted a POST for params " (:params request))
  creation-result)

(defn link-rejected [request]
  (println "xxx Rejected a POST for request " request)
  "Nope!")
