(ns guestbookv3.auth.ring
  (:require
   [clojure.tools.logging :as log]
   [guestbookv3.auth :as auth]
   [reitit.ring :as ring]))

(defn authorized? [roles req]
  (if (seq roles)
    (->> req
         :session
         :identity
         auth/identity->roles
         (some roles)
         boolean)
    (do
      (log/error "roles: " roles " is empty for route: " (:uri req))
      false)))

(defn get-roles-from-match [req]
  (-> req
      (ring/get-match)
      (get-in [:data ::auth/roles] #{})))

(defn wrap-authorized [handler unauthorized-handler]
  (fn [req]
    (if (authorized? (get-roles-from-match req) req)
      (handler req)
      (unauthorized-handler req))))
