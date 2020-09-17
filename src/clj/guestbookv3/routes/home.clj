(ns guestbookv3.routes.home
  (:require
   [guestbookv3.layout :as layout]
   [guestbookv3.db.core :as db]
   [clojure.java.io :as io]
   [guestbookv3.middleware :as middleware]
   [ring.util.response]
   [ring.util.http-response :as response]
   [guestbookv3.validation :refer [validate-message]]
   [guestbookv3.messages :as msg]))

;; Database

(defn save-message! [{:keys [params]}]
  (try
    (msg/save-message! params)
    (response/ok {:status :ok})
    (catch Exception e
      (let [{id :guestbook/error-id
             errors :error} (ex-data e)]
        (case id
          :validation
          (response/bad-request {:errors errors})
          ;; else
          (response/internal-server-error
           {:errors {:server-error ["Failed to save message!"]}}))))))

(defn message-list [_]
  (response/ok (msg/message-list)))

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
   ["/about" {:get about-page}]])

