(ns proxy-router.handler.default-handler
  (:require [integrant.core :as ig]
            [manifold.stream :as s]
            [manifold.deferred :as d]
            [byte-streams :as bs]
            [aleph.tcp :as tcp]
            [clojure.string :as str]))

(defn solve-dest [x {:keys [route-table default-dest]}]
  (let [[line] (take 1 (bs/to-line-seq x))]
    (when line
      (let [[method url proto] (str/split line #" ")]
        (when url
          (println url)
          (if-let [c-opt (some (fn [{:keys [url-pattern dest]}]
                                 (if (re-find url-pattern url) {:host (:host dest) :port (:port dest)}))
                               route-table)]
            c-opt
            (if-let [{:keys [host port]} default-dest]
              {:host host :port port}
              ;; TODO direct-connect
              )))))))

(defn handler [s info routes]
  ;;(println "\n-------------- handler --------------")
  (let [dest       (d/deferred)
        dispatcher (s/stream)]
    (s/connect-via s
                   (fn [x]
                     (when-not (realized? dest)
                       (when-let [r (solve-dest x routes)]
                         (d/success! dest r)))
                     (s/put! dispatcher x))
                   dispatcher)
    (-> dest
        (d/chain (fn [{:keys [host port] :as c-opt}]
                   (when-not (nil? host)
                     (tcp/client c-opt)))
                 (fn [c]
                   (if (nil? c)
                     (s/close! s)
                     (do (s/connect dispatcher c)
                         (s/connect c s)))))
        (d/catch Exception
            #(do (println "whoops, that didn't work:" %)
                 (s/close! s))))))

(defmethod ig/init-key :proxy-router.handler/default-handler
  [_ {:keys [routes] :as options}]
  (fn [s info]
    (handler s info routes)))
