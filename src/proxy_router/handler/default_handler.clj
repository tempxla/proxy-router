(ns proxy-router.handler.default-handler
  (:require [integrant.core :as ig]
            [manifold.stream :as s]
            [manifold.deferred :as d]
            [byte-streams :as bs]
            [aleph.tcp :as tcp]
            [clojure.string :as str]))

(defn solve-host [lines]
  (some #(if (.startsWith (.toLowerCase %) "host")
           (let [[_ host port] (str/split % #":")]
             {:host (str/trim host)
              :port (if (nil? port) 80 (str/trim port))}))
        lines))

(defn solve-dest [x {:keys [route-table default-dest]}]
  (doseq [x (bs/to-line-seq x)]
    (println "|" x "[" (.length x) "]"))
  (let [lines (bs/to-line-seq x)]
    (when-let [line (first lines)]
      (let [[method url proto] (str/split line #" ")]
        (when url
          (println url)
          ;; search url-pattern
          (if-let [c-opt (some (fn [{:keys [url-pattern dest]}]
                                 (if (re-find url-pattern url)
                                   (if (= dest :direct)
                                     (solve-host (next lines))
                                     {:host (:host dest) :port (:port dest)})))
                               route-table)]
            c-opt
            ;; url-pattern not found
            (if (= default-dest :direct)
              (solve-host (next lines))
              (if-let [{:keys [host port]} default-dest]
                {:host host :port port}))))))))

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
