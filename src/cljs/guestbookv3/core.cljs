(ns guestbookv3.core
  (:require [reagent.core :as r]
            [reagent.dom :as dom]
            [ajax.core :refer [GET POST]]
            [guestbookv3.validation :refer [validate-message]]
            [re-frame.core :as rf]))

;; Reframe - initialize
(rf/reg-event-fx
 :app/initialize
 (fn [_ _]
   {:db {:messages/loading? true}
    :dispatch [:messages/load]}))

;; Reframe - messages
(rf/reg-sub
 :messages/loading?
 (fn [db _]
   (:messages/loading? db)))

(rf/reg-event-db
 :messages/set
 (fn [db [_ messages]]
   (-> db
       (assoc :messages/loading? false
              :messages/list messages))))

(rf/reg-event-fx
 :messages/load
 (fn [{:keys [db]} _]
   (GET "/api/messages"
        {:headers {"Accept" "application/transit+json"}
         :handler #(rf/dispatch [:messages/set (:messages %)])})
   {:db (assoc db :messages/loading? true)}))

(rf/reg-sub
 :messages/list
 (fn [db _]
   (:messages/list db [])))

(rf/reg-event-db
 :messages/add
 (fn [db [_ message]]
   (update db :messages/list conj message)))

;; Form fields

(rf/reg-event-db
 :form/set-field
 [(rf/path :form/fields)]
 (fn [fields [_ id value]]
   (assoc fields id value)))

(rf/reg-event-db
 :form/clear-fields
 [(rf/path :form/fields)]
 (fn [_ _]
   {}))

(rf/reg-sub
 :form/fields
 (fn [db _]
   (:form/fields db)))

(rf/reg-sub
 :form/field
 :<- [:form/fields]
 (fn [fields [_ id]]
   (get fields id)))

;; Errors

(rf/reg-event-db
 :form/set-server-error
 [(rf/path :form/server-error)]
 (fn [_ [_ errors]]
   errors))

(rf/reg-sub
 :form/server-errors
 (fn [db _]
   (:form/server-errors db)))

(rf/reg-sub
 :form/validation-errors
 :<- [:form/fields]
 (fn [fields _]
   (validate-message fields)))

(rf/reg-sub
 :form/validation-errors?
 :<- [:form/validation-errors]
 (fn [errors _]
   (not (empty? errors))))

(rf/reg-sub
 :form/errors
 :<- [:form/server-errors]
 :<- [:form/validation-errors]
 (fn [[server validation] _]
   (merge server validation)))

(rf/reg-sub
 :form/error-id
 :<- [:form/errors]
 (fn [errors [_ id]]
   (get errors id)))

;; Messaging

(rf/reg-event-fx
 :message/send!
 (fn [{:keys [db]} [_ fields]]
   (POST "/api/message"
         {:format :json
          :headers {"Accept" "application/transit+json"
                    "x-csrf-token" (.-value (.getElementById js/document "token"))}
          :params fields
          :handler #(do
                      (.log js/console (str "response: " %))
                      (rf/dispatch [:messages/add
                                    (-> fields
                                        (assoc :timestamp (js/Date.)))]))
          :error-handler #(rf/dispatch
                           [:form/set-server-errors
                            (get-in % [:response :errors])])})
   {:db (dissoc db :form/server-errors)}))

(defn get-messages []
  (GET "/api/messages"
       {:headers {"Accept" "application/transit+json"}
        :handler #(rf/dispatch [:messages/set (:messages %)])
        }))

(defn message-list [messages]
  (println messages)
  [:ul.messages
   (for [{:keys [timestamp message name]} @messages]
     ^{:key timestamp}
     [:li
      [:time (.toLocaleString timestamp)]
      [:p message]
      [:p " - " name]])])

;; Components

(defn errors-component [id]
  (when-let [error @(rf/subscribe [:form/error-id id])]
    [:div.notification.is-danger (clojure.string/join error)]))


(defn message-form []
  [:div
   [errors-component :server-error]
   [:div.field
    [:label.label {:for :name} "Name"]
    [errors-component :name]
    [:input.input
     {:type :text
      :name :name 
      :on-change #(rf/dispatch
                   [:form/set-field
                    :name
                    (.. % -target -value)])
      :value @(rf/subscribe [:form/field :name])}]]
   [:div.field
    [:label.label {:for :message} "Message"]
    [errors-component :message]
    [:textarea.textarea
     {:name :message
      :on-change #(rf/dispatch
                   [:form/set-field
                    :message
                    (.. % -target -value)])
      :value @(rf/subscribe [:form/field :message])
      }]]
   [:input.button.is-primary
    {:type :submit
     :disabled @(rf/subscribe [:form/validation-errors?])
     :on-click #(rf/dispatch [:message/send! @(rf/subscribe [:form/fields])] )
     :value "comment"}]])

(defn reload-messages-button []
  (let [loading? @(rf/subscribe [:messages/loading?])]
    [:button.button.is-info.is-fullwidth
     {:on-click #(rf/dispatch [:messages/load])
      :disabled loading?}
     (if loading?
       "Loading messages..."
       "Refresh messages")]))

(defn home []
  (let [messages (rf/subscribe [:messages/list])]
    (fn []
      [:div.content>div.columns.is-centered>div.column.is-two-thirds
       (if @(rf/subscribe [:messages/loading?])
         [:h3 "Loading Messages..."] 
         [:div
          [:div.columns>div.column
           [:h3 "Messages"]
           [message-list messages]]
          [:div.columns>div.column
           [reload-messages-button]]
          [:div.columns>div.column
           [message-form]]])])))

(defn ^:dev/after-load mount-components []
  (rf/clear-subscription-cache!)
  (.log js/console "Mounting Components...")
  (dom/render [#'home] (.getElementById js/document "content"))
  (.log js/console "Components Mounted!"))

(defn init! []
  (.log js/console "Initializing App...")
  (rf/dispatch [:app/initialize])
  (mount-components))
