(ns proxy-router.service.proxy-router
  (:require [duct.logger :as log]
            [integrant.core :as ig]))

(defmethod ig/prep-key :proxy-router.service/proxy-router [_ options]
  (merge {:logger (ig/ref :duct/logger)} options))

(defmethod ig/init-key :proxy-router.service/proxy-router [_ {:keys [logger]}]
  (log/log logger :report ::proxy-router-initiated))
