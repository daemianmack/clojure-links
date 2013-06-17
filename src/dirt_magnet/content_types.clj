(ns dirt-magnet.content-types
  (:require [pedestal.content-negotiation :as cn])
  (:import  (java.io OutputStreamWriter)
            (java.util.zip GZIPOutputStream)))


; Copy-paste from content-negotiation code until function there is no
; longer private or author can recommend a less hacky way of
; establishing an in-API stream-writer fn for custom HTML gzip route.
(defn- stream-writer
  "Returns a function that accepts a clojure object and returns a new
  function that writes the object using printer to an output stream."
  [stream-filter printer]
  (fn [obj]
    (fn [stream]
      (with-open [out (OutputStreamWriter. (stream-filter stream))]
        (binding [*out* out]
          (printer obj))
        (.flush out)))))

(defn wrap-json-dates [orig-fn]
  "Wrap pedestal.content-negotiation/stream-writer so that it doesn't see
   raw timestamps for a json response."
  (let [timestamp->epoch #(reduce-kv
                           (fn [m k v]
                             (assoc m k (if (isa? (class v) java.util.Date)
                                          (.getTime v)
                                          v)))
                           {} %)]
    (fn [obj]
      ;; Accommodate application responses
      ;; but also allow config fns flexibility.
      (cond
       (sequential? obj) (orig-fn (map timestamp->epoch obj))
       (map? obj)        (orig-fn (timestamp->epoch obj))
       :else             (orig-fn obj)))))

(defn with-json-date-wrapping [routes]
  "Modify given routes of content-type json to pass through our wrapper fn first."
  (let [wrap-json-writer (fn [m {:keys [content-type] :as k} orig-fn]
                           (assoc m k (if (= content-type "application/json")
                                        (wrap-json-dates orig-fn)
                                        orig-fn)))]
    (reduce-kv wrap-json-writer {} routes)))

(defn edn+json []
  "Alter the default route-map."
  (with-json-date-wrapping (cn/route-map)))

(defn html+edn+json []
  "Add to the modified default route-map the ability to handle text/html."
  (merge (edn+json)
         {(cn/route {:content-type "text/html" :charset "utf-8" :encoding "identity"}) identity}
         {(cn/route {:content-type "text/html" :charset "utf-8" :encoding "gzip"})     (stream-writer #(GZIPOutputStream. %) println)}))

(def html+edn+json-interceptor
  (cn/content-negotiation (html+edn+json)))
