(ns proxy-router.app-config
  (:require [duct.logger :as log]
            [integrant.core :as ig]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]
            [proxy-router.main :refer [custom-readers]]))

(defmethod ig/prep-key :proxy-router/app-config [_ options]
  (merge {:logger (ig/ref :duct/logger)} options))

(defmethod ig/init-key :proxy-router/app-config [_ {:keys [logger filename]}]
  (when filename
    (let [path       (.getAbsolutePath (io/file (System/getProperty "user.home") ".config" filename))
          app-config (edn/read-string {:readers custom-readers} (slurp path))]
      (log/log logger :report ::initiated path)
      (log/debug logger (with-out-str (newline) (pprint app-config)))
      app-config)))
