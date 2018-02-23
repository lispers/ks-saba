(ns clj-saba.handlers.api
  (:require
    [compojure.core :refer [GET POST defroutes routes context]]
    [monger.query :as q]
    [monger.collection :as mc]
    [config.core :refer [env]]
    [monger.operators :refer :all]
    [monger.conversion :as mcv]))

(defn api-routes [db]
(defn count-in [w k t] (mc/count db t {k (re-pattern (str (if (= k :word) "^") w))}))
  (routes
   (context "/api" []
            (POST "/search" data
                  (let [d ((data :params) :data)]
                    (prn d)
                    (def qry (mcv/from-db-object (q/with-collection db "sitkviskona"
                                                                    (q/find {:word (re-pattern (str "^" (d :kw)))})
                                                                    (q/limit (d :lim))
                                                                    (q/fields [ :word ])
                                                                    ) true))
                    (def qr2 (mc/count db "sitkviskona" {:word (re-pattern (str "^" (d :kw)))}))
                    (let [ls (map #(do {:key (clojure.core/str (% :_id)) :title (% :word) :description (pr-str
                                                                                                        {:w-count (count-in (% :word) :word "sitkviskona") :t-count (count-in (% :word) :translation "sitkviskona") :s-count (count-in (% :word) :cerili "scolio")})}) qry)]
                      {:body (pr-str (if (< qr2 (d :lim)) ls (concat ls [{:title (str "load-more " "(" (- qr2 (d :lim)) ")") :key 0}])))}
                      )
                    )
                  )
            (POST "/get-records" data
                  (let [d (-> data :params :data)]
                    (prn  d)
                    (def qry (mcv/from-db-object (q/with-collection db "sitkviskona"
                                                                    (q/find {:word (re-pattern (str "^" (d :kw)))})
                                                                    (q/fields [:word :translation])
                                                                    ) true))
                    (def qry2 (mcv/from-db-object (q/with-collection db "sitkviskona"
                                                                    (q/find {:translation (re-pattern (d :kw))})
                                                                    (q/fields [:word :translation])
                                                                    ) true))

                   (def qry3 (mcv/from-db-object (q/with-collection db "scolio"
                                                                    (q/find {:cerili (re-pattern (d :kw))})
                                                                    (q/fields [:cigni :tavi :pckari :cerili])
                                                                    ) true))
                    (def list (atom ()))
                    (do
                      (if (> (count-in (d :kw) :word "sitkviskona") 0)
                        (swap! list concat (map #(do {:key (-> % :_id clojure.core/str) :word (-> % :word) :translation (-> % :translation) :table 1}) qry))
                        )
                      (if (> (count-in (d :kw) :translation "sitkviskona") 0)
                        (swap! list concat (map #(do {:key (-> % :_id clojure.core/str) :word (-> % :word) :translation (-> % :translation) :table 2}) qry2))
                        )
                      (if (> (count-in (d :word) :cerili "scolio") 0)
                        (swap! list concat (map #(do {:key (-> % :_id clojure.core/str) :cigni (-> % :cigni) :tavi (-> % :tavi)
                                                      :pckari (-> % :pckari) :cerili (-> % :cerili)
                                                      :table 3}) qry3))
                        )
                      )
                    {:body (-> @list (into []) pr-str)})
                  ))
    )
  )