(ns proxy-router.handler.default-handler-test
  (:require [clojure.test :refer :all]
            [integrant.core :as ig]
            [proxy-router.handler.default-handler :as default-handler]))

(deftest extract-host-port-test
  (is (= nil (default-handler/extract-host-port [])))
  (is (= {:host "example.com" :port 8080}
         (default-handler/extract-host-port (seq ["Date: GMT" "Host: example.com:8080" nil]))))
  (is (= {:host "example.com" :port 80}
         (default-handler/extract-host-port (seq ["Date: GMT" "Host: example.com" nil])))))
