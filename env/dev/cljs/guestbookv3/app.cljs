(ns ^:dev/once guestbookv3.app
  (:require
   [devtools.core :as devtools]
   [guestbookv3.core :as core]))

(enable-console-print!)

(println "loading env/dev/cljs/guestbook/app.cljs...")

(devtools/install!)

(core/init!)
