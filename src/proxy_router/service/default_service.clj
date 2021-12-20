(ns proxy-router.service.default-service
  (:require [duct.logger :as log]
            [integrant.core :as ig]
            [aleph.tcp :as tcp]))

(defmethod ig/prep-key :proxy-router.service/default-service [_ options]
  (merge {:logger (ig/ref :duct/logger)} options))

(defmethod ig/init-key :proxy-router.service/default-service [_ {:keys [logger handler port]}]
  (log/log logger :report ::proxy-router-initiated port)
  {:logger logger
   :server (tcp/start-server handler {:port port})})

(defmethod ig/halt-key! :proxy-router.service/default-service [_ {:keys [logger server]}]
  (log/log logger :report ::proxy-router-halted)
  (.close ^java.io.Closeable server))
