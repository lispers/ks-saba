(ns ^:figwheel-no-load clj-saba.dev
  (:require
    [clj-saba.core :as core]
    [devtools.core :as devtools]))

(devtools/install!)

(enable-console-print!)

(core/init!)
