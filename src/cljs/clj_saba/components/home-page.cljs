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
  (reagent/atom {:lim 5 :open true :compact false})
  )
(defn update-state [key val]
  (swap! app-state assoc-in [key] val)
  )
(def noborder {:border "none" :box-shadow "none"})
(defn make-label [color name]
  [:div {:style {:display "flex" :align-items "center"}} [sa/Label {:circular true :color color :empty true :style {:margin-right 5 :margin-left 5}}] name])
(defn result-renderer [x]
  (let [data (js->clj x :keywordize-keys true)]
    (defn cnt [c]
      (c (cljs.reader/read-string (data :description)))
      )
    (reagent/as-element [:div {:class "ui sel-res" :style {:display "flex"} :id (if (clojure.string/includes? (data :title) "load-more") "load-more")}
                         (data :title) (if (clojure.string/includes? (data :title) "load-more") (if (:loading @app-state) [sa/Loader {:style {:display "flex" :align-self "center"
                                                                                                                    :margin-left 5}  :active true :inline true :size "mini"}])) (if (> (cnt :w-count) 0) (make-label "red" (cnt :w-count)))
                         (if (> (cnt :t-count) 0) (make-label "yellow" (cnt :t-count)))
                         (if (> (cnt :s-count) 0) (make-label "green" (cnt :s-count)))
                         ]))
  )
(defn result-select [x,y]
  (update-state :results [((js->clj y :keywordize-keys true) :result)])
  (((js->clj y :keywordize-keys true) :result) :title)
  )
(defn update-results [val]
  (if (or (< (alength (clojure.string/trim val)) 1) (re-find #"\\|\^|\)|\(" val)) (update-state :results [])
   (do
     (update-state :loading true)
      (go
   (let [response (<! (http/post "/api/search"
                                 ;; parameters
                                 {:with-credentials? false
                                  :json-params {:data {:kw val :lim (:lim @app-state) :compact (:compact @app-state)}}
                                  :as "vector"}))]
     (<! (timeout 100))
     (update-state :results (concat (into [] (cljs.reader/read-string (:body  response)))))
     (update-state :loading false)
     (<! (timeout 100))
     (if (not (= (.indexOf (pr-str :results @app-state) "load-more") -1)) (scroll-to-id "load-more"))
     )))))
(defn get-records [val]
  (go
   (let [response (<! (http/post "/api/get-records"
                                 {:with-credentials? false
                                  :json-params {:data {:kw val}}
                                  :as "vector"}))]
     (update-state :records (into [] (cljs.reader/read-string (:body  response))))
     )
    )
  )
(defn home-page []
  (let [val (atom "")]
    (fn []
      [:div.ui.container
       {:on-click #(do
                    (let [hit (-> %1 .-target .-className)]
                      (or (clojure.string/includes? hit "sel-res") (clojure.string/includes? hit "result") (clojure.string/includes? hit "prompt")(update-state :open false))
                      )
                    )}
       [:div.centered.row {:style {:height "10vh"}}
        [:div.twelve.wide.column
         [:h1  {:style {:text-align "center"}}
          "ცოდნის სისტემა"]
         ]
        ]
       [sa/SegmentGroup {:style (merge {:height "80vh" :width "100%" :display "flex" :flex-direction "column" :margin 0} noborder)}
        [sa/GridRow {:style {:display "flex" :height "fit-content" :top (if (:selected @app-state) 0 "0%") :transition "0.1s"}}
         [:div.centered.row {:style {:display "flex", :flex-direction "column" :margin "0 auto"}}
          [:div {:style {:display "flex"}}
           (make-label "red" "word")
           (make-label "yellow" "translation")
           (make-label "green" "scolio")
           ]
          [:div {:style {:margin 5 :display "flex"}} [sa/Radio {:toggle true
                                                                :checked (:compact @app-state)
                                                                :on-change #(do (update-state :compact (if (not (:compact @app-state)) true false)))
                                                                :style {:margin-right 10}}] (if (:compact @app-state) "compact")]
          [:div {:class "ui search"}
           [:div
            [sa/Search {:name "keyword" :placeholder "" :loading (:loading @app-state)  :value @val
                        :results (:results @app-state)
                        :showNoResults false
                        :fluid true
                        :onFocus #(update-state :open true)
                        :open (:open @app-state)
                        :resultRenderer result-renderer
                        :onResultSelect #(do
                                          (update-state :open true)
                                          (let [sel (((js->clj %2 :keywordize-keys true) :result) :title)]
                                            (if (not (clojure.string/includes? sel "load-more"))
                                              (do
                                               (result-select %1 %2) (reset! val sel) (update-state :selected true) (update-state :open false)
                                                (get-records sel)
                                                )
                                              (do
                                                (.preventDefault %1)
                                                (update-state :lim (+ (:lim @app-state) 5))
                                                (reset! val (-> %2 .-value))
                                                (update-state :kw @val)
                                                (update-results @val)
                                                ))))
                        :onSearchChange #(do  (reset! val (-> %2 .-value))
                                          (update-state :lim 5)
                                          (update-state :kw @val)
                                          (update-state :selected false)
                                          (update-results @val)
                                          )
                        }]
            ]
           ]
          ]
         ]
          (if (> (-> (:records @app-state) count) 0)
            (do
              (def panes
                [
                  {:menuItem "word" :key 1 :render #(reagent/as-element [sa/TabPane "test 123123123 111 word"])}
                  {:menuItem "translation" :key 2 :render #(reagent/as-element [sa/TabPane "test 123123123 111 translation"])}
                  {:menuItem "scolio" :key 3 :render #(reagent/as-element [sa/TabPane "test 123123123 111 scolio"])}
                ]
                )
             [sa/SegmentGroup {:style (merge noborder {:margin-top 10})}
              [sa/Segment  [sa/Tab {:style {:margin "0 auto"} :menu {:secondary true} :panes panes}]]
             ]
              ))
        ]
       [sa/SegmentGroup {:style noborder}
       [sa/Segment {:style {:text-align "center"}}
          [:p {:class "copyright" :style {:padding 15}} "Created by Jaba V Tkemaladze, Akaki V Tkemaladze, Natalia J Tkemaladze, Akaki A Tkemaladze, Lela Gotua © 1999"]
        ]
        ]
       ]
      )
    )
  )