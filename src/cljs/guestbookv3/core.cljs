(ns guestbookv3.core
  (:require
   [reagent.core :as r]
   [reagent.dom :as dom]
   [re-frame.core :as rf]

   [reitit.coercion.spec :as reitit-spec]
   [reitit.frontend :as rtf]
   [reitit.frontend.easy :as rtfe]

   [clojure.string :as string]
   [mount.core :as mount]

   [guestbookv3.routes.app :refer [app-routes]]
   [guestbookv3.websockets :as ws]
   [guestbookv3.auth :as auth]
   [guestbookv3.messages :as messages]
   [guestbookv3.ajax :as ajax]))

;; Reframe - initialize
(rf/reg-event-fx
 :app/initialize
 (fn [_ _]
   {:db {:messages/loading? true
         :session/loading? true}
    :dispatch-n [[:session/load] [:messages/load]]}))

;; Router

(def router
  (rtf/router
   (app-routes)
   {:data {:coercion reitit-spec/coercion}}))

(rf/reg-event-db
 :router/navigated
 (fn [db [_ new-match]]
   (assoc db :router/current-route new-match)))

(rf/reg-sub
 :router/current-route
 (fn [db]
   (:router/current-route db)))

(defn init-routes! []
  (rtfe/start!
   router
   (fn [new-match]
     (when new-match
       (rf/dispatch [:router/navigated new-match])))
   {:use-fragment false}))

;; Components

;; (defn reload-messages-button []
;;   (let [loading? @(rf/subscribe [:messages/loading?])]
;;     [:button.button.is-info.is-fullwidth
;;      {:on-click #(rf/dispatch [:messages/load])
;;       :disabled loading?}
;;      (if loading?
;;        "Loading messages..."
;;        "Refresh messages")]))

(defn navbar []
  (let [burger-active (r/atom false)]
    (fn []
      [:nav.navbar.is-info
       [:div.container
        [:div.navbar-brand
         [:a.navbar-item
          {:href "/"
           :style {:font-weight "bond"}}
          "guestbook"]
         [:span.navbar-burger.burger
          {:data-target "nav-menu"
           :on-click #(swap! burger-active not)
           :class (when @burger-active "is-active")}
          [:span]
          [:span]
          [:span]]]
        [:div#nav-menu.navbar-menu
         {:class (when @burger-active "is-active")}
         [:div.navbar-start
          [:a.navbar-item
           {:href "/"}
           "Home"]]
         [:div.navbar-end
          [:div.navbar-item
           (case @(rf/subscribe [:auth/user-state])
             :loading
             [:div {:style {:width "5em"}}
              [:progress.progress.is-dark.is-small {:max 100} "30%"]]
             
             :authenticated
             [:div.buttons
              [auth/nameplate @(rf/subscribe [:auth/user])]
              [auth/logout-button]]

             :anonymous
             [:div.buttons
              [auth/login-button]
              [auth/register-button]])]]]]])))

(defn page [{{:keys [view name]} :data
             path :path}]
  [:section.section>div.container
   (if view
     [view]
     [:div "No view specified for route: " name " (" path ")"])])

;; App

(defn app []
  (let [current-route @(rf/subscribe [:router/current-route])]
    [:div.app
     [navbar]
     [page current-route]]))

(defn ^:dev/after-load mount-components []
  (rf/clear-subscription-cache!)
  (.log js/console "Mounting Components...")
  (init-routes!)
  (dom/render [#'app] (.getElementById js/document "content"))
  (.log js/console "Components Mounted!"))

(defn init! []
  (.log js/console "Initializing App...")
  (mount/start)
  (rf/dispatch [:app/initialize])
  (mount-components))
