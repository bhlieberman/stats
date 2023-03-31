(ns bhlie.ui.app
  (:require [reagent.core :as r]
            [goog.dom :as gdom]
            ["react-dom/client" :refer [createRoot]]))

(defonce root (createRoot (gdom/getElement "root")))

(defn main []
  [:div "Hello from React"])

(defn ^:dev/after-load init []
  (.render root (r/as-element [main])))