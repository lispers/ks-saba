(ns clj-saba.components
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as reagent :refer [atom]]
            [clojure.browser.dom :as cbd]
            [reagent.core :as reagent]
            [soda-ash.core :as sa]
            [cljs-http.client :as http]
            [secretary.core :as secretary :include-macros true]
            [accountant.core :as accountant]
            [cljs.core.async :refer [<! timeout]]
            [clj-saba.util :refer [scroll-to-id]])
  )
(def app-state
  (reagent/atom {:lim 5})
  )
(defn update-state [key val]
  (swap! app-state assoc-in [key] val)
  )
(defn result-renderer [x]
  (let [data (js->clj x :keywordize-keys true)]
    (reagent/as-element [:div {:class "ui" :id (if (= (data :title) "load-more") "load-more")} (data :title)]))
  )
(defn result-select [x,y]
  (update-state :results [((js->clj y :keywordize-keys true) :result)])
  (((js->clj y :keywordize-keys true) :result) :title)
  )
(defn update-results [val]
  (go
   (let [response (<! (http/post "/api/search"
                                 ;; parameters
                                 {:with-credentials? false
                                  :json-params {:data {:kw val :lim (:lim @app-state)}}
                                  :as "vector"}))]
     (update-state :loading true)
     (<! (timeout 100))
     (update-state :results (concat (into [] (cljs.reader/read-string (:body  response))) [{:key 0 :title "load-more"}] ))
     (update-state :loading false)
     )))
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
                        :open true
                        :resultRenderer result-renderer
                        :onResultSelect #(do
                                          (let [sel (((js->clj %2 :keywordize-keys true) :result) :title)]
                                            (prn sel)
                                            (if (not (= sel "load-more"))
                                              (do
                                               (result-select %1 %2) (reset! val sel))
                                              (do
                                                (.preventDefault %1)
                                                (update-state :lim (+ (:lim @app-state) 5))
                                                (reset! val (-> %2 .-value))
                                                (update-state :kw @val)
                                                (update-results @val)
                                                (scroll-to-id "load-more")
                                                )
                                              )
                                            )
                                          )
                        :onSearchChange #(do  (reset! val (-> %2 .-value))
                                          (update-state :lim 5)
                                          (update-state :kw @val)
                                          (update-results @val)
                                          )
                        }]
            ]
           ]
          ]
         ]
        ]
       [:div.centered.row {:style {:height "10vh" :position "fixed" :bottom 30 :display "flex", :align-items "center"}}
        [:p {:class "copyright" :style {:padding 15}} "Created by Jaba V Tkemaladze, Akaki V Tkemaladze, Natalia J Tkemaladze, Akaki A Tkemaladze, Lela Gotua © 1999"]

        ]
       ]

      )

    )
  )