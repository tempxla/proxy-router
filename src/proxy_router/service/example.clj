(ns proxy-router.service.example
  (:require [duct.logger :as log]
            [integrant.core :as ig]))

(defmethod ig/prep-key :proxy-router.service/example [_ options]
  (merge {:logger (ig/ref :duct/logger)} options))

(defmethod ig/init-key :proxy-router.service/example [_ {:keys [logger]}]
  (log/log logger :report ::example-initiated))
