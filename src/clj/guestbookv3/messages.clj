(ns guestbookv3.messages
  (:require
   [guestbookv3.db.core :as db]
   [guestbookv3.validation :refer [validate-message]]
   [clojure.tools.logging :as log]))

(defn message-list []
  {:messages  (vec (db/get-messages))})

(defn save-message! [{:keys [login]} message]
  (if-let [errors (validate-message message)]
    (throw (ex-info "Message is invalid"
                    {:guestbook/error-id :validation
                     :errors errors}))
    (db/save-message! (assoc message :author login))))


(defn messages-by-author [author]
  {:messages (vec (db/get-messages-by-author {:author author}))})
