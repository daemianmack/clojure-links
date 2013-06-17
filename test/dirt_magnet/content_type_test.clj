(ns dirt-magnet.content-type-test
  (:require [clojure.test :refer [deftest is testing]]
            [bond.james :as bond]
            [io.pedestal.service.test :refer :all]
            [io.pedestal.service.http :as bootstrap]
            [dirt-magnet.service :as service]
            [dirt-magnet.config :as config]
            [dirt-magnet.links :as links]
            [dirt-magnet.storage :as storage]
            [ring.util.response :refer [response]]
            [clojure.data.json :as json]
            [clojure.edn :as edn]))

(def service
  (::bootstrap/service-fn (bootstrap/create-servlet service/service)))


(defn test-acceptance-fn [req] (let [params (service/keyword-all-params req)]
                                 (= "xyzzy" (:password params))))

(def good-body {:source "grue"
                :url "http://grues.com"
                :password "xyzzy"})

(def bad-body (assoc good-body :password "pencil"))

(def fake-link-feed
  (list {:source "s"
         :url "http://example.com/"
         :created_at (java.sql.Timestamp. (.getTime (java.util.Date.)))}))


(defn response-is-content-type? [type resp]
  (.startsWith ((:headers resp) "Content-Type") type))

(defn response-for-links-listing [headers]
  (with-redefs [links/get-links (constantly fake-link-feed)]
    (response-for service :get "/" :headers headers)))

(deftest test-links-listing-returns-correct-response-type
  (testing "request for html returns html"
    (is (response-is-content-type? "text/html"
                                   (response-for-links-listing {"Accept" "text/html"}))))

  (testing "request for json returns json"
    (is (response-is-content-type? "application/json"
                                   (response-for-links-listing {"Accept" "application/json"}))))

  (testing "request for edn returns edn"
    (is (response-is-content-type? "application/edn"
                                   (response-for-links-listing {"Accept" "application/edn"})))))


(defn response-for-creating-link [post-body post-headers]
  (with-redefs [config/link-acceptable? test-acceptance-fn
                config/link-accepted    (constantly (response fake-link-feed))]
    (bond/with-stub [storage/insert-into-table
                     links/fetch-title-if-html]
      (response-for service
                    :post "/links"
                    :body post-body
                    :headers post-headers))))

(deftest test-link-creation-returns-correct-response-type
  (testing "request for json returns json"
    (let [resp (response-for-creating-link (json/write-str good-body)
                                           {"Content-Type" "application/json"
                                            "Accept"       "application/json"})]
      (is (response-is-content-type? "application/json" resp))))

  (testing "return-type-defaults-to-edn"
    (let [resp (response-for-creating-link
                (str good-body)
                {"Content-Type" "application/edn"})]
      (is (response-is-content-type? "application/edn" resp))))

  (testing "posting-json-requesting-edn-returns-edn"
    (let [resp (response-for-creating-link
                (json/write-str good-body)
                {"Content-Type" "application/json"
                 "Accept"       "application/edn"})]
      (is (response-is-content-type? "application/edn" resp)))))

;; TODO: Use data.generators or at least custom JSON serializer to
;; automate the expected half?
(def json-response-fns
  [["String.",
    "\"String.\""]
   [{:status "Map." :at (java.util.Date. 1)},
    "{\"at\":1, \"status\":\"Map.\"}"]
   [(repeat 2 {:status "Vector o' maps." :at (java.sql.Timestamp. 1)}),
    "[{\"at\":1, \"status\":\"Vector o' maps.\"},\n {\"at\":1, \"status\":\"Vector o' maps.\"}]"]])

(deftest test-semi-arbitrary-data-response-yields-correct-json-data
  (doseq [[attempt expected] json-response-fns]
    (with-redefs [config/link-rejected (constantly (response attempt))]
      (testing "json return path properly mangles response"
        (let [resp (response-for-creating-link (json/write-str bad-body)
                                               {"Content-Type" "application/json"
                                                "Accept"       "application/json"})]
          (is (= (:body resp) expected)))))))
