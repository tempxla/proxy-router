(ns proxy-router.main
  (:gen-class)
  (:require [duct.core :as duct]
            [proxy-router.handler.default-handler :refer [custom-readers]]))

(duct/load-hierarchy)

(defn -main [& args]
  (let [keys     (or (duct/parse-keys args) [:duct/daemon])
        profiles [:duct.profile/prod]]
    (-> (duct/resource "proxy_router/config.edn")
        (duct/read-config custom-readers)
        (duct/exec-config profiles keys))
    (System/exit 0)))
