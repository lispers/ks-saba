(ns clj-saba.prod
  (:require [clj-saba.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
