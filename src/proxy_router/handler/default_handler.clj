(ns proxy-router.handler.default-handler
  (:require [integrant.core :as ig]
            [manifold.stream :as s]
            [manifold.deferred :as d]
            [byte-streams :as bs]
            [aleph.tcp :as tcp]
            [clojure.edn :as edn]
            [clojure.string :as str]))

(defn solve-dest [x {:keys [route-table default-route]}]
  (let [[line] (take 1 (bs/to-line-seq x))]
    (when line
      (let [[method url proto] (str/split line #" ")]
        (when url
          (println url)
          (println route-table)
          (if-let [c-opt (reduce (fn [_ {:keys [url-pattern dest-host dest-port]}]
                                   (if (re-find url-pattern url)
                                     (reduced {:host dest-host :port dest-port})))
                                 nil
                                 route-table)]
            c-opt
            (if-let [{:keys [dest-host dest-port]} default-route]
              {:host dest-host :port dest-port}
              ;; TODO direct-connect
              )))))))

(defn handler [s info config]
  (println "\n-------------- handler --------------")
  (let [dest       (d/deferred)
        dispatcher (s/stream)]
    (s/connect-via s
                   (fn [x]
                     (when-not (realized? dest)
                       (when-let [r (solve-dest x config)]
                         (d/success! dest r)))
                     (s/put! dispatcher x))
                   dispatcher)
    (-> dest
        (d/chain (fn [c-opt]
                   (tcp/client c-opt))
                 (fn [c]
                   (s/connect dispatcher c)
                   (s/connect c s)))
        (d/catch Exception
            #(do (println "whoops, that didn't work:" %)
                 (s/close! s))))))

(defmethod ig/init-key :proxy-router.handler/default-handler
  [_ {:keys [config] :as options}]
  (fn [s info]
    (handler s info config)))
