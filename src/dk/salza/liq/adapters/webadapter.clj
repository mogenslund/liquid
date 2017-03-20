(ns dk.salza.liq.adapters.webadapter
  (:require [dk.salza.liq.tools.util :as util]
            [dk.salza.liq.keys :as keys]
            [clojure.string :as str])
  (:import [java.io OutputStream]
           [java.net InetSocketAddress]
           [com.sun.net.httpserver HttpExchange HttpHandler HttpServer]))

(def server (atom nil))
(def output (atom "aaaaaaa"))
(def input (promise))


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
                            var xmlhttp = new XMLHttpRequest();
                            document.getElementById(\"w0-r2\").innerHTML= evt.keyCode;
                            //alert(\"KeyCode: \" + evt.keyCode);
                            
                            if (evt.keyCode == 9) {
                              var xhttp = new XMLHttpRequest();
                              xhttp.onreadystatechange = function() {
                                if (this.readyState == 4 && this.status == 200) {
                                  document.getElementById(\"w0-r4\").innerHTML = this.responseText;
                                }
                              };
                              xhttp.open(\"GET\", \"key/tab\", true);
                              xhttp.send();
                            } else if (evt.keyCode == 77) {
                              var xhttp = new XMLHttpRequest();
                              xhttp.onreadystatechange = function() {
                                if (this.readyState == 4 && this.status == 200) {
                                  document.getElementById(\"w0-r4\").innerHTML = this.responseText;
                                }
                              };
                              xhttp.open(\"GET\", \"key/m\", true);
                              xhttp.send();
                            } else {
                              var xhttp = new XMLHttpRequest();
                              xhttp.onreadystatechange = function() {
                                if (this.readyState == 4 && this.status == 200) {
                                  document.getElementById(\"w0-r4\").innerHTML = this.responseText;
                                }
                              };
                              xhttp.open(\"GET\", \"output\", true);
                              xhttp.send();
                            }
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
      (let [path (.getPath (.getRequestURI exchange))
            response (cond (= path "/output") @output
                           (= path "/key/tab") (do (deliver input :tab) @output)
                           (= path "/key/m") (do (deliver input :m) @output)
                           :else (html))]
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
  (let [r @input]
    (def input (promise))
    r))
  ;(let [r (read-line)]
  ;  :i))

(defn print-lines
  [lines]
  (let [simple (clojure.string/join "\n" (map #(clojure.string/join "" (filter string? (% :line))) lines))]
    (reset! output (str "<pre>" simple "</pre>"))))

(def adapter
  {:init init
   :rows rows
   :columns columns
   :wait-for-input wait-for-input
   :print-lines print-lines
   :reset not-implemented-yet
   :quit quit})