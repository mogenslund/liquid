(ns dk.salza.liq.adapters.serveradapter
  (:require [clojure.core.server :as server]))

(def inputserver (atom nil))

(defn action
  [& args]
  (println "Action:" args))


(defn wait-for-input
  []
  (when (not @inputserver)
    (reset! inputserver (server/start-server
                        {:port 8520                   
                         :name "inputserver"
                         :accept dk.salza.liq.adapters.serveradapter/action}
                        )))
  (Thread/sleep 1000))
