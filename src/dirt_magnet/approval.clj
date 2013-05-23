(ns dirt-magnet.approval)

(defn creation-approved? [{:keys [params]}]
  (println "Approved a POST for params " params)
  (= "professor-falken" (:password params)))

(defn creation-denied [request]
  (println "Denied a POST for request " request)
  "Nope!")
