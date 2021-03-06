(ns clj-saba.middleware
  (:require [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [prone.middleware :refer [wrap-exceptions]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.json :as middleware]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]))
(defn wrap-middleware [handler]
  (-> handler
      (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false))
      (wrap-keyword-params)
      (middleware/wrap-json-params)
      wrap-exceptions
      wrap-reload))
