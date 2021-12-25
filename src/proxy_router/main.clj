(ns proxy-router.main
  (:gen-class)
  (:require [duct.core :as duct]))

(def custom-readers
  {'proxy-router/regex re-pattern})

(duct/load-hierarchy)

(defn -main [& args]
  (let [keys     (or (duct/parse-keys args) [:duct/daemon])
        profiles [:duct.profile/prod]]
    (-> (duct/resource "proxy_router/config.edn")
        (duct/read-config custom-readers)
        (duct/exec-config profiles keys))
    (System/exit 0)))
