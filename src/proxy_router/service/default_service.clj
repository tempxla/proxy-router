(ns proxy-router.service.default-service
  (:require [duct.logger :as log]
            [integrant.core :as ig]
            [aleph.tcp :as tcp]))

(defmethod ig/prep-key :proxy-router.service/default-service [_ options]
  (merge {:logger (ig/ref :duct/logger)} options))

(defmethod ig/init-key :proxy-router.service/default-service
  [_ {:keys [logger handler port app-config]}]
  (let [port (or (get-in app-config [:proxy-router.service/default-service :port]) port)]
    (log/log logger :report ::initiated (str "port:" port))
    {:logger logger
     :server (tcp/start-server handler {:port port})
     :port port}))

(defmethod ig/halt-key! :proxy-router.service/default-service [_ {:keys [logger server port]}]
  (log/log logger :report ::halted (str "port:" port))
  (.close ^java.io.Closeable server))
