(ns dirt-magnet.links-test
  (:require [clojure.test :refer [deftest is]]
            [dirt-magnet.storage :as storage]
            [dirt-magnet.fixtures :as fixtures]
            [dirt-magnet.links :as links]
            [clj-http.client :as client]
            [bond.james :as bond]))

(deftest title-response
  (with-redefs [client/get (constantly fixtures/body-response)]
    (is (= (links/get-title fixtures/url) fixtures/title))))

(deftest title-response-fallback
  (with-redefs [client/get #(throw (Exception. "404: Bam Spiceweasel"))]
    (is (= nil (links/get-title fixtures/url)))))

(deftest is-html
  (with-redefs [client/head (constantly fixtures/html-headers)]
    (is (= true (links/is-html? fixtures/url)))))

(deftest is-not-html
  (with-redefs [client/head (constantly fixtures/jpeg-headers)]
    (is (= false (links/is-html? fixtures/url)))))

(deftest fetch-title-falls-back-to-persisted-link-for-image
  (let [fake-data {:is_image true :url "http://foo.com"}]
    (with-redefs [links/get-link (constantly fake-data)]
      (is (= fake-data (links/fetch-title-if-html 42))))))

(deftest fetch-title-falls-back-to-persisted-link-for-unresponsive-url
  (let [fake-data {:is_image false :url "http://foo.com"}]
    (with-redefs [links/get-link (constantly fake-data)
                  links/get-title (constantly nil)]
      (is (= fake-data (links/fetch-title-if-html 42))))))

(deftest fetch-title-updates-and-returns-full-link-for-responsive-link-with-title
  (let [fake-data {:is_image false :url "http://foo.com" :title "HURRAY IT'S A TITLE"}]
    (with-redefs [links/get-link (constantly fake-data)
                  links/get-title (constantly (:title fake-data))]
      (bond/with-stub [storage/update-table]
        (is (= fake-data (links/fetch-title-if-html 42)))
        (is (= 1 (count (bond/calls storage/update-table))))))))

