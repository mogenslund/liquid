(ns dk.salza.liq.adapters.webadapter
  (:require [dk.salza.liq.tools.util :as util]
            [dk.salza.liq.renderer :as renderer]
            [dk.salza.liq.editor :as editor]
            [dk.salza.liq.window :as window]
            [clojure.string :as str])
  (:import [java.io OutputStream]
           [java.net InetSocketAddress]
           [com.sun.net.httpserver HttpExchange HttpHandler HttpServer]))

(def server (atom nil))
(def input (atom (promise)))
(def ^:private dimensions (atom {:rows 40 :columns 80}))
(def ^:private autoupdate (atom false))


(def ^:private style
     "body {
        //background-color: #181818;
        background-color: #080808;
        margin: 0;
        margin-top: 50;
        color: #e4e4ef
      }

      span.type1 {color: #ffdd33;}
      span.type2 {color: #95a99f;}
      span.type3 {color: #ffdd33;}
      span.comment {color: #cc8c3c;}
      span.string {color: #73c936;}
      span.stringst {color: #73c936;}
      span.bgplain {background-color: #181818;}
      span.bgcursor1 {background-color: green;}
      span.bgcursor2 {background-color: blue;}
      span.bghl {background-color: yellow;}
      span.bgselection {background-color: purple;}
      span.bgstatusline {background-color: #000000;}

      div.row {
        font-family: monospace;
        font-size: 16px;
        line-height: 16px;
        white-space: pre;
        vertical-align: middle;
      }

      th, td {
        border-collapse: collapse;
        border: none;
        padding: 0;
        border-spacing: 0;
        margin: 0px auto;
        vertical-align: middle;
        background-color: #181818;
      }

      table {
        border-collapse: collapse;
        border: solid;
        padding: 0;
        border-spacing: 0;
        margin: 0px auto;
        border-color: #000000;
        background-color: #181818;
      }")

(defn- javascript
  []
  (str "

   function updateLine(line) {
     var parts = line.match(/(w\\d+-r\\d+):(.*)/);
     if (document.getElementById(parts[1]).innerHTML != parts[2]) {
       document.getElementById(parts[1]).innerHTML = parts[2];
     }
   }

   function mapk(letter, ctrl, meta) {
     if (letter.length >= 2) {letter = letter.toLowerCase();} 
     if (letter == 'tab') {letter = '\\t';}
     if (letter == 'enter') {letter = '\\n';}
     if (letter == 'pagedown') {letter = 'pgdn';}
     if (letter == 'pageup') {letter = 'pgup';}
     var ctrlstr = '';
     if (ctrl) {
       ctrlstr = 'C-';
     }
     var metastr = '';
     if (meta) {
       metastr = 'M-';
     }
     if (letter.length == 1) {letter = letter.charCodeAt(0);}
     return ctrlstr + metastr + letter;
   }

   var xhttp = new XMLHttpRequest();

   xhttp.onreadystatechange = function() {
     if (this.readyState == 4 && this.status == 200) {
       this.responseText.split('\\n').forEach(updateLine)
     }
   };

   function updategui(evt) {
     if (evt) {
       evt.preventDefault();
       evt.stopPropagation();
     }
     xhttp.abort();

     if (evt) {
       xhttp.open(\"GET\", \"key/\" + mapk(evt.key, evt.ctrlKey, evt.altKey), true);
     } else {
       xhttp.open(\"GET\", \"output\", true);
     }
     xhttp.send();
   }

   document.onkeydown = updategui;

   " (when @autoupdate "setInterval(() => updategui(false), 20);") "
   updategui(false);

   function init() {

    
     
   }"))


(defn row
  [window r]
  (str "<div class=\"row\" id=\"w" window "-r" r "\"></div>"))

(defn- html
  []
  (str "<html><head><style>" style "</style><script type=\"text/javascript\">" (javascript) "</script></head>"
       "  <body onload=\"init();\">"
       "<table><tr>"
       "<td>" (str/join "\n" (map #(row 0 %) (range 1 (inc (@dimensions :rows))))) "</td>"
       "<td>" (str/join "\n" (map #(row 1 %) (range 1 (inc (@dimensions :rows))))) "</td>"
       "</tr></table>"
       "<div id=\"tmp\"></div>"
       "</body></html>"))


(def old-lines (atom {}))

(defn- convert-line
  [line]
  (let [key (str "w" (if (= (line :column) 1) "0" "1") "-r" (line :row))
        content (str "<span class=\"bgstatusline\"> </span><span class=\"plain bgplain\">"
                  (str/join (for [c (line :line)]
                    (if (string? c)
                      c
                      (str "</span><span class=\"" (name (c :face)) " bg" (name (c :bgface)) "\">" (or (c :char) "?")))))
                  "</span>")]
    (str key ":" content)))


(defn- get-output
  []
  (let [lineslist (renderer/render-screen)]
        (str/join "\n" 
          (filter #(not= "" %) (pmap convert-line (apply concat lineslist))))))

(defn- str2char
  [s]
  (if-let [m (re-matches #"((?:C-)?(?:M-)?)?(\d+)" s)]
    (str (nth m 1) (char (Integer/parseInt (nth m 2))))
    s))

(def ^:private handler
  (proxy [HttpHandler] []
    (handle
      [exchange]
      (let [path (.getPath (.getRequestURI exchange))
            response (cond (= path "/output") (get-output)
                           (re-find #"^/key/" path) (let [num (re-find #"(?<=/key/).*" path)
                                                          c (str2char num)] 
                                                      (editor/handle-input c)
                                                      (get-output)
                                                      )
                           :else (html))]
        (.sendResponseHeaders exchange 200 (count (.getBytes response)))
        (let [ostream (.getResponseBody exchange)]
          (.write ostream (.getBytes response))
          (.close ostream)))
      )))

(defn- quit
  []
  (.stop @server)
  (System/exit 0))
      

(defn- init
  [port]
  (reset! server (HttpServer/create (InetSocketAddress. port) 0))
  (.createContext @server "/" handler)
  (.setExecutor @server nil)
  (.start @server))

(defn- wait-for-input
  []
  (let [r @@input]
    (println "Input" r)
    (reset! input (promise))
    r))

(defn- print-lines
  [lines]
  )

(defn adapter
  [rows columns auto]
  (reset! autoupdate auto)
  (reset! dimensions {:rows rows :columns columns})
  {:init init
   :rows (fn [] rows)
   :columns (fn [] columns)
   :wait-for-input wait-for-input
   :print-lines print-lines
   :reset #(do)
   :quit quit})