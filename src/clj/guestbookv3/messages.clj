(ns guestbookv3.messages
  (:require
   [guestbookv3.db.core :as db]
   [guestbookv3.validation :refer [validate-message]]))

(defn message-list []
  {:messages  (vec (db/get-messages))})

(defn save-message! [message]
  (if-let [errors (validate-message message)]
    (throw (ex-info "Message is invalid"
                    {:guestbook/error-id :validation
                     :errors errors}))
    (db/save-message! message)))
