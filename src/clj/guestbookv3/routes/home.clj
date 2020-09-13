(ns guestbookv3.routes.home
  (:require
   [guestbookv3.layout :as layout]
   [guestbookv3.db.core :as db]
   [clojure.java.io :as io]
   [guestbookv3.middleware :as middleware]
   [ring.util.response]
   [ring.util.http-response :as response]
   [guestbookv3.validation :refer [validate-message]]))

;; Database

(defn save-message! [{:keys [params]}]
  (if-let [errors (validate-message params)]
    (response/bad-request {:errors errors})
    (try
      (db/save-message! params)
      (response/ok {:status :ok})
      (catch Exception e
        (response/internal-server-error
         {:errors {:server-error ["Failed to save message!"]}})))))

(defn message-list [_]
  (response/ok {:messages (vec (db/get-messages))}))

;; Routes

(defn home-page [request]
  (layout/render
   request
   "home.html"))

(defn about-page [request]
  (layout/render request "about.html"))



(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get home-page}]
   ["/message" {:post save-message!}]
   ["/about" {:get about-page}]
   ["/messages" {:get message-list}]])

