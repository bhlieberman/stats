(ns bhlie.pages.home
  (:require [selmer.parser :refer [render render-file]]))

(selmer.parser/set-resource-path! "public/templates/")

(def home-template (render-file "index.html" {:home {:banner "Hello world!"}}))