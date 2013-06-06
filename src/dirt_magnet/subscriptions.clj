(ns dirt-magnet.subscriptions
  (:require [io.pedestal.service.log :as log]
            [io.pedestal.service.http.sse :as sse]
            [dirt-magnet.templates :as templates]))


(def ^{:doc "Map of IDs to SSE contexts"} subscribers (atom {}))

(defn remove-bad-context [bad-context]
  (fn [subscribers] (into {} (remove (fn [[_ ctxt]] (= bad-context ctxt)) subscribers))))

(defn remove-subscriber
  "Remove `context` from subscribers map and end the event stream."
  [bad-context]
  (log/info :msg "removing subscriber")
  (swap! subscribers (remove-bad-context bad-context))
  (sse/end-event-stream bad-context))

(defn send-to-subscriber
  "Send `msg` as event to event stream represented by `context`. If
  send fails, removes `context` from subscribers map."
  [context msg]
  (log/info :msg (str "sending msg " msg))
  (try
    (sse/send-event context "message" msg)
    (catch java.io.IOException ioe
      (remove-subscriber context))))

(defn send-to-subscribers
  "Send `msg` to all event streams in subscribers map."
  [msg]
  (doseq [sse-context (vals @subscribers)]
    (send-to-subscriber sse-context msg)))

(defn add-to-subscribers [id context]
  (swap! subscribers assoc id context))
