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
        (let [d ((data :params) :data)]
          (prn d)
          (def qry (mcv/from-db-object (q/with-collection db "sitkviskona"
                                                           (q/find {:word (re-pattern (str "^" (d :kw)))})
                                                           (q/limit (d :lim))
                                                           (q/fields [ :word ])
                                                           ) true))
          (def qr2 (mc/count db "sitkviskona" {:word (re-pattern (str "^" (d :kw)))}))
          (defn count-in [w k t] (mc/count db t {k (re-pattern (str "^" w))}))
          (let [ls (map #(do {:key (clojure.core/str (% :_id)) :title (% :word) :description (pr-str
              {:w-count (count-in (% :word) :word "sitkviskona") :t-count (count-in (% :word) :translation "sitkviskona") :s-count (count-in (% :word) :cerili "scolio")})}) qry)]
            {:body (pr-str (if (< qr2 (d :lim)) ls (concat ls [{:title (str "load-more " "(" (- qr2 (d :lim)) ")") :key 0}])))}
            )
          )
          )
  )