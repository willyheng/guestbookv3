(ns guestbookv3.routes.websockets
  (:require [clojure.tools.logging :as log]
            [org.httpkit.server :as http-kit]
            [guestbookv3.messages :as msg]
            [guestbookv3.middleware :as middleware]
            [mount.core :refer [defstate]]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer [get-sch-adapter]]))

;; (defonce channels (atom #{}))

(defstate socket
  :start (sente/make-channel-socket!
          (get-sch-adapter)
          {:user-id-fn (fn [ring-req]
                         (get-in ring-req [:params :client-id]))}))

(defn send! [uid message]
  (println "Sending message: " message)
  ((:send-fn socket) uid message))

(defmulti handle-message (fn [{:keys [id]}] id))

(defmethod handle-message :default
  [{:keys [id]}]
  (log/debug "Received unrecognized websocket event type: " id)
  {:error (str "Unrecognized websocket event type: " (pr-str id))
   :id id})

(defmethod handle-message :message/create!
  [{:keys [?data uid] :as message}]
  (let [response (try
                   (msg/save-message! ?data)
                   (assoc ?data :timestamp (java.util.Date.))
                   (catch Exception e
                     (let [{id :guestbook/error-id
                            errors :errors} (ex-data e)]
                       (case id
                         :validation
                         {:errors errors}
                         ;;else
                         {:errors
                          {:server-error ["Failed to save message!"]}}))))]
    (if (:errors response)
      (do
        (log/debug "Failed to save message: " ?data)
        response)
      (do 
        (doseq [uid (:any @(:connected-uids socket))]
          (send! uid [:message/add response]))
        {:success true}))))

(defn receive-message! [{:keys [id ?reply-fn] :as message}]
  (log/debug "Got message with id: " id)
  (let [reply-fn (or ?reply-fn (fn [_]))]
    (when-some [response (handle-message message)]
      (reply-fn response))))

;; (defn connect! [channel]
;;   (log/info "Channel opened")
;;   (swap! channels conj channel))

;; (defn disconnect! [channel status]
;;   (log/info "Channel closed")
;;   (swap! channels disj channel))

;; (defn handle-message! [channel ws-message]
;;   (let [message (edn/read-string ws-message)
;;         response (try
;;                    (msg/save-message! message)
;;                    (assoc message :timestamp (java.util.Date.))
;;                    (catch Exception e
;;                      (let [{id :guestbook/error-id
;;                             errors :errors} (ex-data e)]
;;                        (case id
;;                          :validation
;;                          {:errors errors}
;;                          ;;else
;;                          {:errors {:server-error ["Failed to save message!"]}}))))]
;;     (if (:errors :response)
;;       (http-kit/send! channel (pr-str response))
;;       (doseq [channel @channels]
;;         (http-kit/send! channel (pr-str response))))))

;; (defn handler [request]
;;   (http-kit/with-channel request channel
;;     (connect! channel)
;;     (http-kit/on-close channel (partial disconnect! channel))
;;     (http-kit/on-receive channel (partial handle-message! channel))))

(defstate channel-router
  :start (do (sente/start-chsk-router!
              (:ch-recv socket)
              #'receive-message!)
             (pr-str (:ajax-get-or-ws-handshake-fn socket)))
  :stop (when-let [stop-fn channel-router]
          (stop-fn)))

(defn websocket-routes []
  ["/ws"
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]
    :get (:ajax-get-or-ws-handshake-fn socket)
    :post (:ajax-post-fn socket)}])
