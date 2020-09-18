(ns guestbookv3.routes.services
  (:require
   [guestbookv3.messages :as msg]
   [guestbookv3.middleware :as middleware]
   [ring.util.http-response :as response]
   [reitit.swagger :as swagger]
   [reitit.swagger-ui :as swagger-ui]
   [reitit.ring.coercion :as coercion]
   [reitit.coercion.spec :as spec-coercion]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.exception :as exception]
   [reitit.ring.middleware.multipart :as multipart]
   [reitit.ring.middleware.parameters :as parameters]
   [guestbookv3.middleware.formats :as formats]))

(defn service-routes []
  ["/api"
   {:middleware [;; query-params & form-params
                 parameters/parameters-middleware
                 ;; content-negotiation
                 muuntaja/format-negotiate-middleware
                 ;; encoding response body
                 muuntaja/format-response-middleware
                 ;; exception handling
                 exception/exception-middleware
                 ;; decoding request body
                 muuntaja/format-request-middleware
                 ;; coercing response bodys
                 coercion/coerce-response-middleware
                 ;; coercing request parameters
                 coercion/coerce-request-middleware
                 ;; Multipart params
                 multipart/multipart-middleware]
    :muuntaja formats/instance
    :coercion spec-coercion/coercion
    :swagger {:id ::api}}
   ["" {:no-doc true}
    ["/swagger.json"
     {:get (swagger/create-swagger-handler)}]
    ["/swagger-ui*"
     {:get (swagger-ui/create-swagger-ui-handler
            {:url "/api/swagger.json"})}]]
   ["/messages" {:get
                 {:responses
                  {200
                   {:body ;; Data Spec for response body
                    {:messages
                     [{:id pos-int?
                       :name string?
                       :message string?
                       :timestamp inst?}]}}}
                  :handler
                  (fn [_]
                    (response/ok (msg/message-list)))}}]
   ["/message" {:post
                {:parameters
                 {:body ;; Data spec for request body parameters
                  {:name string?
                   :message string?}}
                 
                 :responses
                 {200 {:body map?}
                  500 {:errors map?}}

                 :handler
                 (fn [{{params :body} :parameters}]
                   (try
                     (msg/save-message! params)
                     (response/ok {:status :ok})
                     (catch Exception e
                       (let [{id :guestbook/error-id
                              errors :errors} (ex-data e)]
                         (case id
                           :validation
                           (response/bad-request {:errors errors})
                           ;; else
                           (response/internal-server-error
                            {:errors {:server-error ["Failed to save message!"]}})
                           )))))}}]])
