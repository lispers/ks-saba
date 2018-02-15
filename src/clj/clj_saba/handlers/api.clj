(ns clj-saba.handlers.api
  (:require
    [compojure.core :refer [GET POST defroutes]]
    [monger.query :as q]
    [monger.collection :as mc]
    [config.core :refer [env]]
    [monger.operators :refer :all]
    [monger.conversion :as mcv]))

(defn api-routes [db]
  (POST "/api/search" data
        (let [kw ((data :params) :kw)]
          (def qry (mcv/from-db-object (q/with-collection db "sitkviskona"
                                                           (q/find {:word (re-pattern (str "^" kw))})
                                                           (q/limit 5)
                                                           (q/fields [ :word ])
                                                           ) true))
          {:body (pr-str (map-indexed #(do
                                        ;(merge (update %2 :_id (fn [x] (clojure.core/str %1))) )
                                        {:key (clojure.core/str (%2 :_id)) :title (%2 :word) }
                                        ) qry))}
          )
          )

  )