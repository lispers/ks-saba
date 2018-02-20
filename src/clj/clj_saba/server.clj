(ns clj-saba.server
  (:require [clj-saba.handler :refer [app]]
            [config.core :refer [env]]
            [ring.adapter.jetty :refer [run-jetty]])
  (:gen-class))

 (defn -main [& args]
   (let [port (Integer/parseInt (or (env :port) "3366"))]
     (run-jetty app {:port port :join? false})))
