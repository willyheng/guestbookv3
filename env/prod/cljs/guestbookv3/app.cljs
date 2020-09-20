(ns guestbookv3.app
  (:require
   [guestbookv3.core :as core]))

;; ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
