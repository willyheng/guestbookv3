(ns guestbookv3.core
  (:require [reagent.core :as r]
            [reagent.dom :as dom]
            [ajax.core :refer [GET POST]]
            [guestbookv3.validation :refer [validate-message]]
            [re-frame.core :as rf]
            [guestbookv3.websockets :as ws]
            [mount.core :as mount]))

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

(rf/reg-fx
 :ajax/get
 (fn [{:keys [url success-event error-event success-path]}]
   (GET url
        (cond-> {:headers {"Accept" "application/transit+json"}}
          success-event (assoc :handler
                               #(rf/dispatch
                                 (conj success-event
                                       (if success-path
                                         (get-in % success-path)
                                         %))))
          error-event (assoc :error-handler
                             #(rf/dispatch
                               (conj error-event %)))))))

(rf/reg-event-fx
 :messages/load
 (fn [{:keys [db]} _]
   {:db (assoc db :messages/loading? true)
    :ajax/get {:url "api/messages",
               :success-path [:messages]
               :success-event [:messages/set]}}))

;; (rf/reg-event-fx
;;  :messages/load
;;  (fn [{:keys [db]} _]
;;    (GET "/api/messages"
;;         {:headers {"Accept" "application/transit+json"}
;;          :handler #(rf/dispatch [:messages/set (:messages %)])})
;;    {:db (assoc db :messages/loading? true)}))

(rf/reg-sub
 :messages/list
 (fn [db _]
   (:messages/list db [])))

(rf/reg-event-db
 :message/add
 (fn [db [_ message]]
   (update db :messages/list conj message)))

(rf/reg-event-fx
 :message/send!-called-back
 (fn [_ [_ {:keys [success errors]}]]
   (if success
     {:dispatch [:form/clear-fields]}
     {:dispatch [:form/set-server-errors errors]})))

(rf/reg-event-fx
 :message/send!
 (fn [{:keys [db]} [_ fields]] 
   {:db (dissoc db :form/server-errors)
    :ws/send! {:message [:message/create! fields]
               :timeout 10000
               :callback-event [:message/send!-called-back]}}))

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
 :form/set-server-errors
 [(rf/path :form/server-errors)]
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

; Handle response from websockets
(defn handle-response! [response]
  (if-let [errors (:errors response)]
    (rf/dispatch [:set-server-errors errors])
    (do
      (rf/dispatch [:message/add response])
      (rf/dispatch [:form/clear-fields]))))

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

(defn text-input [{val :value
                   attrs :attrs
                   :keys [on-save]}]
  (let [draft (r/atom nil)
        value (r/track #(or @draft @val ""))]
    (fn []
      [:input.input
       (merge attrs
              {:type :text
               :on-focus #(reset! draft (or @val ""))
               :on-blur (fn []
                          (on-save (or @draft ""))
                          (reset! draft nil))
               :on-change #(reset! draft (.. % -target -value))
               :value @value})])))

(defn text-area [{val :value
                  attrs :attrs
                  :keys [on-save]}]
  (let [draft (r/atom nil)
        value (r/track #(or @draft @val ""))]
    (fn []
      [:textarea.textarea
       (merge attrs
              {:on-focus #(reset! draft (or @val ""))
               :on-blur (fn []
                          (on-save (or @draft ""))
                          (reset! draft nil))
               :on-change #(reset! draft (.. % -target -value))
               :value @value})])))

(defn message-form []
  [:div
   [errors-component :server-errors]
   [:div.field
    [:label.label {:for :name} "Name"]
    [errors-component :name]
    [text-input {:attrs {:name :name}
                 :value (rf/subscribe [:form/field :name])
                 :on-save #(rf/dispatch [:form/set-field :name %])}]]
   [:div.field
    [:label.label {:for :message} "Message"]
    [errors-component :message]
    [text-area {:attrs {:name :message}
                :value (rf/subscribe [:form/field :message])
                :on-save #(rf/dispatch [:form/set-field :message %])}]]
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
  (mount/start)
  (rf/dispatch [:app/initialize])
  (mount-components))
