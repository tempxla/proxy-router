(ns proxy-router.handler.default-handler
  (:require [integrant.core :as ig]
            [duct.logger :as log]
            [manifold.stream :as s]
            [manifold.deferred :as d]
            [byte-streams :as bs]
            [aleph.tcp :as tcp]
            [clojure.string :as str]))

;; (defn response-ok []
;;   (str/join "\r\n"
;;             ["HTTP/1.1 200 OK"
;;              "Server: proxy-router"
;;              (str "Date: " (.format java.time.format.DateTimeFormatter/RFC_1123_DATE_TIME
;;                                     (java.time.OffsetDateTime/now java.time.ZoneOffset/UTC)))
;;              "Content-Type: text/plain"
;;              "Content-Length: 2"
;;              ""
;;              "ok"]))

(defn extract-host-port [lines]
  (some #(if (.startsWith (.toLowerCase %) "host")
           (let [[_ host port] (str/split % #":")]
             {:host (str/trim host)
              :port (if (nil? port) 80 (Integer/parseInt (str/trim port)))}))
        lines))

(defn solve-dest [x {:keys [route-table default-dest]}]
  ;; (doseq [x (bs/to-line-seq x)]
  ;;   (println "|" x "[" (.length x) "]"))
  (let [lines (bs/to-line-seq x)]
    (when-let [line (first lines)]
      (let [[method url proto] (str/split line #" ")]
        (when url
          ;; search url-pattern
          (if-let [c-opt (some (fn [{:keys [url-pattern dest]}]
                                 (if (re-find url-pattern url)
                                   (if (= dest :direct)
                                     (assoc (extract-host-port (next lines)) :direct? true)
                                     {:host (:host dest) :port (:port dest)})))
                               route-table)]
            (assoc c-opt :url url)
            ;; url-pattern not found
            (-> (if (= default-dest :direct)
                  (assoc (extract-host-port (next lines)) :direct? true)
                  (if-let [{:keys [host port]} default-dest]
                    {:host host :port port}))
                (assoc :url url))))))))

(defn handler [s info logger routes]
  (let [dest       (d/deferred)
        dispatcher (s/stream)]
    (s/connect-via s
                   (fn [x]
                     (if (realized? dest)
                       (s/put! dispatcher x)
                       (if-let [r (solve-dest x routes)]
                         (do (d/success! dest r)
                             (if (and (:direct? r) (= (:port r) 443))
                               (do (log/info logger ::dispatch (str (:url r) " --> [CONNECT]"))
                                   (s/put! s (bs/to-byte-buffer "HTTP/1.1 200 Connection established\r\n\r\n"))
                                   (d/success-deferred true))
                               (do (log/info logger ::dispatch (str (:url r) " --> " (str (:host r) ":" (:port r))))
                                   (s/put! dispatcher x))))
                         (s/put! dispatcher x))))
                   dispatcher)
    (-> dest
        (d/chain (fn [{:keys [host port] :as c-opt}]
                   (when-not (nil? host)
                     (tcp/client {:host host :port port})))
                 (fn [c]
                   (if (nil? c)
                     (s/close! s)
                     (do (s/connect dispatcher c)
                         (s/connect c s)))))
        (d/catch Exception
            #(do (log/error logger ::dispatch (str "whoops, that didn't work: " %))
                 (s/close! s))))))

(defmethod ig/prep-key :proxy-router.handler/default-handler [_ options]
  (merge {:logger (ig/ref :duct/logger)} options))

(defmethod ig/init-key :proxy-router.handler/default-handler
  [_ {:keys [logger routes app-config] :as options}]
  (let [routes (or (get-in app-config [:proxy-router.handler/default-handler :routes]) routes)]
    (log/log logger :report ::initiated)
    (log/debug logger (with-out-str (newline) (clojure.pprint/pprint routes)))
    (fn [s info]
      (handler s info logger routes))))
