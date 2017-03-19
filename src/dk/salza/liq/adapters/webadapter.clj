(ns dk.salza.liq.adapters.webadapter
  (:require [dk.salza.liq.tools.util :as util]
            [dk.salza.liq.keys :as keys]
            [clojure.string :as str])
  (:import [java.io OutputStream]
           [java.net InetSocketAddress]
           [com.sun.net.httpserver HttpExchange HttpHandler HttpServer]))

(def server (atom nil))
(def style
     "body {
        background-color: #111111;
        margin: 0;
        color: #dddddd
      }

      span.type2 {
        color: green;
      }

      span.type1 {
        color: yellow;
      }

      span.statusline {
        color: yellow;
        background-color: #444444;
        color: #dddddd
      }

      div.row {
        font-family: monospace;
        font-size: 16px;
        line-height: 16px;
        white-space: pre;
      }

      div.row::before {
        background-color: #444444;
        content: \" \";
      }
      table, th, td {
        border-collapse: collapse;
        border: none;
        padding: 0;
        border-spacing: 0;
        margin: 0;
      }")

(def javascript
  "function init() {
     document.onkeydown = function(evt) {
                            document.getElementById(\"w0-r2\").innerHTML= evt.keyCode;
                            //alert(\"KeyCode: \" + evt.keyCode);
                          }}")

(defn row
  [window r]
  (str "<div class=\"row\" id=\"w" window "-r" r "\"></div>"))

(defn html
  []
  (str "<html><head><style>" style "</style><script type=\"text/javascript\">" javascript "</script></head>"
       "  <body onload=\"init();\">"
       "a "
       (System/currentTimeMillis)
       "<table><tr>"
       "<td>" (str/join "\n" (map #(row 0 %) (range 10))) "</td>"
       "<td>" (str/join "\n" (map #(row 1 %) (range 10))) "</td>"
       "</tr></table>"
       "</body></html>"))

; java -cp /home/molu/resources/clojure-1.8.0.jar:/home/molu/m/lib2  clojure.main -m dk.salza.liqscratch.server
; (slurp "http://localhost:8520")
; http://www.rgagnon.com/javadetails/java-have-a-simple-http-server.html
; http://unixpapa.com/js/key.html

(def handler
  (proxy [HttpHandler] []
    (handle
      [exchange]
      (let [response (html)]
        (.sendResponseHeaders exchange 200 (count (.getBytes response)))
        (let [ostream (.getResponseBody exchange)]
          (.write ostream (.getBytes response))
          (.close ostream))))))

(defn rows
  []
  10)

(defn columns
  []
  120)

(defn quit
  []
  (.stop @server)
  (System/exit 0))
      

(defn init
  [& args]
  (reset! server (HttpServer/create (InetSocketAddress. 8520) 0))
  (.createContext @server "/" handler)
  (.setExecutor @server nil)
  (.start @server))

(defn not-implemented-yet
  [& args]
  )

(defn wait-for-input
  []
  (let [r (read-line)]
    :k))

(def adapter
  {:init init
   :rows rows
   :columns columns
   :wait-for-input wait-for-input
   :print-lines not-implemented-yet
   :reset not-implemented-yet
   :quit quit})