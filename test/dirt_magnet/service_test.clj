(ns dirt-magnet.service-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.edn :refer [read-string]]
            [io.pedestal.service.test :refer :all]
            [io.pedestal.service.http :as bootstrap]
            [io.pedestal.service.http.sse :as sse]
            [dirt-magnet.service :as service]
            [dirt-magnet.config :as config]
            [dirt-magnet.links :as links]
            [dirt-magnet.storage :as storage]
            [dirt-magnet.subscriptions :as subs]
            [ring.util.response :refer [response]]
            [bond.james :as bond]))

(def service
  (::bootstrap/service-fn (bootstrap/create-servlet service/service)))


(defn test-acceptance-fn [req] (let [params (service/keyword-all-params req)]
                                 (= "xyzzy" (:password params))))

(defn stub-accepted-fn [_ _] :accepted)
(defn stub-rejected-fn [_]   :rejected)

(def good-body {:source "grue"
                :url "http://grues.com"
                :password "xyzzy"})

(def bad-body
  (update-in good-body [:password] (constantly "fangs")))

(def headers
  {"Content-Type" "application/x-www-form-urlencoded"})

(defn form-encode [body]
  (apply str (interpose "&" (mapv (fn [[k v]] (str (name k) "=" v)) body))))


(deftest accept-create-link
  (with-redefs [config/link-acceptable? test-acceptance-fn
                config/link-accepted    stub-accepted-fn]
    (bond/with-stub [links/insert-link
                     service/fetch-link-title-later]
      (is (= :accepted (service/create-link {:params good-body})))
      (is (= 1 (count (bond/calls links/insert-link))))
      (is (= 1 (count (bond/calls service/fetch-link-title-later))))
      (is (= ["grue" "http://grues.com"]
             ((comp :args first) (bond/calls links/insert-link)))))))

(deftest reject-create-link
  (with-redefs [config/link-acceptable? test-acceptance-fn
                config/link-rejected    stub-rejected-fn]
    (is (= :rejected (service/create-link {:params bad-body})))))

(deftest exercise-view-path
  (with-redefs [storage/query (constantly (list good-body))]
    (is (.contains (:body (response-for service :get "/"))
                   (:url good-body)))))

(deftest exercise-create-path
  (with-redefs [config/link-acceptable? test-acceptance-fn
                config/link-accepted    (constantly (response {:status-code 201
                                                               :message "Created"}))]
    (bond/with-stub [storage/insert-into-table
                     service/fetch-link-title-later]
      (is (= 201 (-> (response-for service
                                   :post "/links"
                                   :body (form-encode good-body)
                                   :headers headers)
                     :body
                     read-string
                     :status-code)))
      (is (= 1 (count (bond/calls storage/insert-into-table))))
      (is (= 1 (count (bond/calls service/fetch-link-title-later))))
      (is (= {:is_image false
              :source "grue"
              :url "http://grues.com"}
             (select-keys ((comp second :args first)
                           (bond/calls storage/insert-into-table))
                          [:is_image :source :url]))))))

(deftest removing-bad-subscriber-contexts
  (let [fake-data (atom {:a 'a :b 'b :c 'c})
        msg       (atom "hi")]
    (with-redefs [subs/subscribers fake-data
                  sse/send-event (fn [ctxt _ _]
                                   (when (= ctxt 'b)
                                     (throw (java.io.IOException.))))]
      (bond/with-stub [sse/end-event-stream]
        (subs/send-to-subscribers @msg)
        (is (= (dissoc @fake-data :b) @subs/subscribers))))))

