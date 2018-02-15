(ns clj-saba.components
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as reagent :refer [atom]]
            [clojure.browser.dom :as cbd]
            [reagent.core :as reagent]
            [soda-ash.core :as sa]
            [cljs-http.client :as http]
            [secretary.core :as secretary :include-macros true]
            [accountant.core :as accountant]
            [cljs.core.async :refer [<! timeout]])
  )
(def app-state
  (reagent/atom {})
  )
(defn update-state [key val]
  (swap! app-state assoc-in [key] val)
  )
(defn result-renderer [x]
  (reagent/as-element [:div {:class "ui"} ((js->clj x :keywordize-keys true) :title)])
  )
(defn result-select [x,y]
  (update-state :results [((js->clj y :keywordize-keys true) :result)])
  (((js->clj y :keywordize-keys true) :result) :title)
  )
(defn home-page []
  (let [val (atom "")]
    (fn []
      [:div.ui.grid
       [:div.centered.row {:style {:height "10vh"}}
        [:div.twelve.wide.column
         [:h1  {:style {:text-align "center"}}
          "ცოდნის სისტემა"]
         ]
        ]
       [:div.centered.row {:style {:height "80vh"}}
        [:div.ten.wide.field {:style {:display "flex", :height "fit-content", :top "40%", :position "absolute"}}
         [:div.centered.row
          [:div {:class "ui search"}
           [:div {:class "ui icon input"}
            ;(atom-search val)
            [sa/Search {:name "keyword" :placeholder "" :loading (:loading @app-state)  :value @val
                        :results (:results @app-state)
                        :showNoResults false
                        :fluid true
                        :resultRenderer result-renderer
                        :onResultSelect #(reset! val (result-select %1 %2))
                        :onSearchChange #(do  (reset! val (-> %2 .-value))
                                          (update-state :kw @val)
                                          (go
                                           (let [response (<! (http/post "/api/search"
                                                                         ;; parameters
                                                                         {:with-credentials? false
                                                                          :json-params {:kw @val}
                                                                          :as "vector"}))]
                                             (update-state :loading true)
                                             (<! (timeout 100))
                                             (update-state :results (into [] (cljs.reader/read-string (:body  response))))
                                             (update-state :loading false)
                                             ))
                                          )
                        }]
            ]
           ]
          ]
         ]
        ]
       [:div.centered.row {:style {:height "10vh"}}
        [:p {:class "copyright"} "Created by Jaba V Tkemaladze, Akaki V Tkemaladze, Natalia J Tkemaladze, Akaki A Tkemaladze, Lela Gotua © 1999"]
        ]
       ]

      )

    )
  )