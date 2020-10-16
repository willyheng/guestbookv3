(ns guestbookv3.routes.app
  (:require
   #?@(:clj [[guestbookv3.layout :as layout]
             [guestbookv3.middleware :as middleware]]
       :cljs [[guestbookv3.views.home :as home]
              [guestbookv3.views.author :as author]])))

#?(:clj
   (defn home-page [request]
     (layout/render
      request
      "home.html")))

(defn app-routes []
  [""
   #?(:clj {:middleware [middleware/wrap-csrf]
            :get home-page})
   ["/"
    (merge
     {:name ::home}
     #?(:cljs
        {:view #'home/home}))]
   ["/user/:user"
    (merge
     {:name ::author}
     #?(:cljs {:view #'author/author}))]])
