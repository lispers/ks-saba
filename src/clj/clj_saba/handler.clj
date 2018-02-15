(ns clj-saba.handler
  (:require
    [compojure.core :refer [GET POST defroutes]]
    [monger.core :as mg]
    [monger.collection :as mc]
    [monger.conversion :as mcv]
    [compojure.route :refer [not-found resources]]
    [hiccup.page :refer [include-js include-css html5]]
    [clj-saba.middleware :refer [wrap-middleware]]
    [config.core :refer [env]]
    [hiccup.page :refer [include-js include-css html5]]
    [clj-saba.handlers.api :refer [api-routes]])
  )
(def mount-target
  [:div#app
      [:h3 "ClojureScript has not been compiled!"]
      [:p "please run "
       [:b "lein figwheel"]
       " in order to start the compiler"]])

(defn head []
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1"}]
   (include-css (if (env :dev) "/css/site.css" "/css/site.min.css"))])

(defn loading-page []
  (html5
    (head)
    [:body {:class "body-container"}
     mount-target
     (include-js "/js/app.js")]))
(let [conn (mg/connect)
        db   (mg/get-db conn (env :database-name))
        scolio "scolio"
        sk "sitkviskona"
        ]
  (defroutes routes
  (GET "/" [] (loading-page))
  (GET "/about" [] (loading-page))
  (api-routes db)
  (resources "/")
  (not-found "Not Found")))
(def app (wrap-middleware #'routes))
