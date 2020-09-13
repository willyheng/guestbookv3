(ns guestbookv3.validation
  (:require
   [struct.core :as st]))

;; Validation
(def message-schema
  [[:name
    st/required
    st/string]
   [:message
    st/required
    st/string
    {:message "Message must contain at least 10 characters"
     :validate (fn [msg] (> (count msg) 10))}]])

(defn validate-message [params]
  (first (st/validate params message-schema)))
