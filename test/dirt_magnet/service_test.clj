(ns dirt-magnet.service-test
  (:require [clojure.test :refer [deftest is]]
            [io.pedestal.service.test :refer :all]
            [io.pedestal.service.http :as bootstrap]
            [dirt-magnet.service :as service]
            [dirt-magnet.config :as config]
            [dirt-magnet.links :as links]
            [dirt-magnet.storage :as storage]
            [ring.util.response :refer [response]]
            [bond.james :as bond]))

(def service
  (::bootstrap/service-fn (bootstrap/create-servlet service/service)))


(defn test-acceptance-fn [{:keys [params]}] (= "xyzzy" (:password params)))
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
    (bond/with-stub [links/store-link]
      (is (= :accepted (service/create-link {:params good-body})))
      (is (= 1 (count (bond/calls links/store-link))))
      (is (= {:url "http://grues.com"
              :source "grue"}
             ((comp first :args first) (bond/calls links/store-link)))))))

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
                config/link-accepted    (fn [_ r] (merge r {:status 201}))]
    (bond/with-stub [storage/insert-into-table
                     links/post-process-link]
      (is (= 201 (:status (response-for service
                                        :post "/links"
                                        :body (form-encode good-body)
                                        :headers headers))))
      (is (= 1 (count (bond/calls storage/insert-into-table))))
      (is (= 1 (count (bond/calls links/post-process-link))))
      (is (= {:is_image false
              :source "grue"
              :url "http://grues.com"}
             (select-keys ((comp second :args first)
                           (bond/calls storage/insert-into-table))
                          [:is_image :source :url]))))))
