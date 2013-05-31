(ns dirt-magnet.links-test
  (:require [clojure.test :refer [deftest is]]
            [dirt-magnet.fixtures :as fixtures]
            [dirt-magnet.links :as links]
            [clj-http.client :as client]))

(deftest title-response
  (with-redefs [client/get (constantly fixtures/body-response)]
    (is (= (links/get-title fixtures/url) fixtures/title))))

(deftest title-response-fallback
  (with-redefs [client/get #(throw (Exception. "404: Bam Spiceweasel"))]
    (is (= (links/get-title fixtures/url) fixtures/url))))

(deftest is-html
  (with-redefs [client/head (constantly fixtures/html-headers)]
    (is (= true (links/is-html? fixtures/url)))))

(deftest is-not-html
  (with-redefs [client/head (constantly fixtures/jpeg-headers)]
    (is (= false (links/is-html? fixtures/url)))))


