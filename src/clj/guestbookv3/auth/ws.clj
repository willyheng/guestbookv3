(ns guestbookv3.auth.ws
  (:require [guestbookv3.auth :as auth]))

(defn authorized? [roles-by-id msg]
  (boolean
   (some (roles-by-id (:id msg) #{})
         (-> msg
             :session
             :identity
             (auth/identity->roles)))))
