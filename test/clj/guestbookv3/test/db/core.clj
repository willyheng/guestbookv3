(ns guestbookv3.test.db.core
  (:require
   [guestbookv3.db.core :refer [*db*] :as db]
   [java-time.pre-java8]
   [luminus-migrations.core :as migrations]
   [clojure.test :refer :all]
   [clojure.java.jdbc :as jdbc]
   [guestbookv3.config :refer [env]]
   [mount.core :as mount]))

(use-fixtures
  :once
  (fn [f]
    (mount/start
     #'guestbookv3.config/env
     #'guestbookv3.db.core/*db*)
    (migrations/migrate ["migrate"] (select-keys env [:database-url]))
    (f)))

(deftest test-guestbook
  (jdbc/with-db-transaction [t-conn *db*]
    (jdbc/db-set-rollback-only! t-conn)
    (is (= 1 (db/save-message!
              t-conn
              {:name    "Willy"
               :message "Hello World!"})))
    (is (= {:name    "Willy"
            :message "Hello World!"}
           (-> (db/get-messages t-conn {})
               (first)
               (select-keys [:name :message]))))))
