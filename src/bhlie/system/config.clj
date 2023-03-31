(ns bhlie.system.config
  (:require [clojure.java.io :as io] 
            [com.stuartsierra.component :as component]
            [com.stuartsierra.component.repl
             :refer [set-init]]
            [cuerdas.core :as c]
            [muuntaja.core :as m]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [next.jdbc :as jdbc]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [reitit.ring :as r]
            [bhlie.pages.home :refer [home-template]]
            [com.slothrop.statcast.batter :refer [send-req!]]))

(defn select-pi [db]
  (jdbc/execute! (:connection db) ["select pi() as pi"]))

(defn kebabify [handler]
  (fn [{:keys [game_date_lt game_date_gt] :as req}]
    (let [kebab (apply #(update-in req [:params %] c/kebab) [game_date_gt game_date_lt])
          res (handler kebab)]
      res)))

(defn splash []
  (slurp (io/resource "public/templates/splash.html")))

(defn routes [db]
  (r/ring-handler
   (r/router ["/"
              ["splash" {:handler (fn [_] {:body (splash) :status 200})}]
              ["pi" {:handler (fn [_] {:body (select-pi db) :status 200})}]
              ["statcast"
               ["/batter" {:handler (fn [{:keys [query-params]}]
                                      (let [body (reduce-kv (fn [m k v] (assoc m (keyword (c/kebab k)) v)) {} query-params)]
                                        {:body (send-req! body)
                                         :status 200}))}]]
              ["assets/*" {:handler (r/create-resource-handler {:root "public/assets"})}]
              ["" {:handler (fn [_] {:body home-template :status 200})}]]
             {:data {:muuntaja m/instance
                     :middleware [muuntaja/format-middleware wrap-params wrap-keyword-params]}})))

(defrecord Database [dbname dbtype]
  component/Lifecycle
  (start [this]
    (println "opening connection to H2")
    (let [conn (jdbc/get-connection {:dbname dbname :dbtype dbtype})]
      (assoc this :connection conn)))
  (stop [this]
    (println "closing connection to H2")
    (assoc this :connection nil)))

(defrecord Jetty [port handler db]
  component/Lifecycle
  (start [this]
    (println "starting Jetty server on port" port)
    (assoc this :http-server (-> db handler (run-jetty {:port port :join? false}))))
  (stop [this]
    (println "shutting down Jetty")
    (.stop (:http-server this))
    this))

(defn web-server [] (component/using (map->Jetty {:port 3001 :handler routes}) [:db]))

(defn new-system [_]
  (component/system-map :db (map->Database {:dbname "h2" :dbtype "h2"})
                        :server (web-server)))

(set-init new-system)

(keyword (c/kebab "game_date_lt"))