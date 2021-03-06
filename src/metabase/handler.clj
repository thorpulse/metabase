(ns metabase.handler
  "Top-level Metabase Ring handler."
  (:require [metabase.middleware
             [auth :as mw.auth]
             [exceptions :as mw.exceptions]
             [json :as mw.json]
             [log :as mw.log]
             [misc :as mw.misc]
             [security :as mw.security]
             [session :as mw.session]]
            [metabase.routes :as routes]
            [ring.middleware
             [cookies :refer [wrap-cookies]]
             [keyword-params :refer [wrap-keyword-params]]
             [params :refer [wrap-params]]]))

(def app
  "The primary entry point to the Ring HTTP server."
  ;; ▼▼▼ POST-PROCESSING ▼▼▼ happens from TOP-TO-BOTTOM
  (->
   #'routes/routes                         ; the #' is to allow tests to redefine endpoints
   mw.exceptions/catch-uncaught-exceptions ; catch any Exceptions that weren't passed to `raise`
   mw.exceptions/catch-api-exceptions      ; catch exceptions and return them in our expected format
   mw.log/log-api-call
   mw.security/add-security-headers        ; Add HTTP headers to API responses to prevent them from being cached
   mw.json/wrap-json-body                  ; extracts json POST body and makes it avaliable on request
   mw.json/wrap-streamed-json-response     ; middleware to automatically serialize suitable objects as JSON in responses
   wrap-keyword-params                     ; converts string keys in :params to keyword keys
   wrap-params                             ; parses GET and POST params as :query-params/:form-params and both as :params
   mw.session/bind-current-user            ; Binds *current-user* and *current-user-id* if :metabase-user-id is non-nil
   mw.session/wrap-current-user-id         ; looks for :metabase-session-id and sets :metabase-user-id if Session ID is valid
   mw.session/wrap-session-id              ; looks for a Metabase Session ID and assoc as :metabase-session-id
   mw.auth/wrap-api-key                    ; looks for a Metabase API Key on the request and assocs as :metabase-api-key
   mw.misc/maybe-set-site-url              ; set the value of `site-url` if it hasn't been set yet
   mw.misc/bind-user-locale                ; Binds *locale* for i18n
   wrap-cookies                            ; Parses cookies in the request map and assocs as :cookies
   #_wrap-session                            ; reads in current HTTP session and sets :session key TODO - don't think we need this
   mw.misc/add-content-type                ; Adds a Content-Type header for any response that doesn't already have one
   mw.misc/wrap-gzip                       ; GZIP response if client can handle it
   ))
;; ▲▲▲ PRE-PROCESSING ▲▲▲ happens from BOTTOM-TO-TOP
