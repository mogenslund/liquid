(ns dk.salza.liq.adapters.htmladapter
  (:require [dk.salza.liq.tools.util :as util]
            [dk.salza.liq.keys :as keys]
            [dk.salza.liq.editor :as editor]
            [dk.salza.liq.window :as window]
            [clojure.string :as str])
  (:import [java.io OutputStream]
           [java.net InetSocketAddress]
           [com.sun.net.httpserver HttpExchange HttpHandler HttpServer]))


(def server (atom nil))
(def input (atom (promise)))
(def dimensions (atom {:rows 40 :columns 80}))
(def autoupdate (atom false))


(def style
     "body {
        //background-color: #181818;
        background-color: #080808;
        margin: 0;
        margin-top: 50;
        font-family: monospace;
        font-size: 16px;
        line-height: 16px;
        white-space: pre;
        color: #e4e4ef
      }

      span.type1 {
        color: #ffdd33;
      }

      span.type2 {
        color: #95a99f;
      }

      span.type3 {
        color: #ffdd33;
      }

      span.comment {
        color: #cc8c3c;
      }

      span.string {
        color: #73c936;
      }

      span.bgplain {
        background-color: #181818;
      }

      span.bgcursor1 {
        background-color: green;
      }

      span.bgcursor2 {
        background-color: blue;
      }

      span.bgselection {
        background-color: purple;
      }

      span.bgstatusline {
        background-color: #000000;
      }

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

(defn javascript
  []
  (str "

   var xhttp = new XMLHttpRequest();

   function init() {

     function mapk(letter, ctrl, meta) {
      if (letter.length >= 2) {letter = letter.toLowerCase();} 
       var ctrlstr = '';
       if (ctrl) {
         ctrlstr = 'C-';
       }
       var metastr = '';
       if (meta) {
         metastr = 'M-';
       }

       var keymap = new Object();
       keymap[' '] = 'space';
       keymap['!'] = 'exclamation';
       keymap['\"'] = 'quote';
       keymap['#'] = 'hash';
       keymap['$'] = 'dollar';
       keymap['%'] = 'percent';
       keymap['&'] = 'ampersand';
       keymap[\"'\"] = 'singlequote';
       keymap['('] = 'parenstart';
       keymap[')'] = 'parenend';
       keymap['*'] = 'asterisk';
       keymap['+'] = 'plus';
       keymap[','] = 'comma';
       keymap['-'] = 'dash';
       keymap['.'] = 'dot';
       keymap['/'] = 'slash';
       keymap[':'] = 'colon';
       keymap[';'] = 'semicolon';
       keymap['<'] = 'lt';
       keymap['='] = 'equal';
       keymap['>'] = 'gt';
       keymap['?'] = 'question';
       keymap['@'] = 'at';
       keymap['['] = 'bracketstart';
       keymap[']'] = 'bracketend';
       keymap['^'] = 'hat';
       keymap['{'] = 'bracesstart';
       keymap['_'] = 'underscore';
       keymap['\\\\'] = 'backslash';
       keymap['|'] = 'pipe';
       keymap['}'] = 'bracesend';
       keymap['~'] = 'tilde';
       keymap['¤'] = 'curren';
       keymap['´'] = 'backtick';
       keymap['Å'] = 'caa';
       keymap['Æ'] = 'cae';
       keymap['Ø'] = 'coe';
       keymap['å'] = 'aa';
       keymap['æ'] = 'ae';
       keymap['ø'] = 'oe';
       return ctrlstr + metastr + (keymap[letter] || letter);
     }

     xhttp.onreadystatechange = function() {
       if (this.readyState == 4 && this.status == 200) {
         document.getElementById(\"app\").innerHTML = this.responseText;
       }
     };

     function updategui(evt) {
       if (evt) {
         evt.preventDefault();
         evt.stopPropagation();
       }

       if (evt) {
         xhttp.open(\"GET\", \"key/\" + mapk(evt.key, evt.ctrlKey, evt.altKey), true);
       } else {
         xhttp.open(\"GET\", \"output\", true);
       }
       xhttp.send();
     }

     document.onkeydown = updategui;
     " (when @autoupdate "setInterval(() => updategui(false), 200);") "
     updategui(false);
     
   }"))

(defn row
  [window r]
  (str "<div class=\"row\" id=\"w" window "-r" r "\"></div>"))

(defn html
  []
  (str "<html><head><style>" style "</style><script type=\"text/javascript\">" (javascript) "</script></head>"
       "  <body onload=\"init();\">"
       "  <div id=\"app\">"
       "</body></html>"))

(defn convert-line
  [line]
  (str "<div class=\"row\"><span class=\"bgstatusline\"> </span><span class=\"plain bgplain\">"
    (str/join (for [c (line :line)] (if (string? c) c (str "</span><span class=\"" (name (c :face)) " bg" (name (c :bgface)) "\">"))))
    "</span></div>"
  ))

(defn get-output
  []
  (let [windows (reverse (editor/get-windows))
        buffers (map #(editor/get-buffer (window/get-buffername %)) windows)
        lineslist (doall (map #(window/render %1 %2) windows buffers))]
    (str ;"<html><head><style>" style "</style><script type=\"text/javascript\">" javascript "</script></head>"
         ;"  <body onload=\"init();\">"
         "<table><tr>"
         (str/join "\n" (for [lines lineslist] (str "<td>" (str/join "\n" (map convert-line lines)) "</td>")))
         "</tr></table>"
         "<div id=\"tmp\"></div>"
         ;"</body></html>"
         )))

(def handler
  (proxy [HttpHandler] []
    (handle
      [exchange]
      (let [path (.getPath (.getRequestURI exchange))
            response (cond (= path "/output") (get-output)
                           (re-find #"^/key/" path) (let [num (re-find #"(?<=/key/).*" path)
                                                          c (keyword num)] 
                                                      (editor/handle-input c)
                                                      (get-output))
                           :else (html))]
        (.sendResponseHeaders exchange 200 (count (.getBytes response)))
        (let [ostream (.getResponseBody exchange)]
          (.write ostream (.getBytes response))
          (.close ostream)))
      ;(reset! output (promise))
      )))

(defn quit
  []
  (.stop @server)
  (System/exit 0))
      

(defn init
  [port]
  (reset! server (HttpServer/create (InetSocketAddress. port) 0))
  (.createContext @server "/" handler)
  (.setExecutor @server nil)
  (.start @server))

(defn wait-for-input
  []
  (let [r @@input]
    (println "Input" r)
    (reset! input (promise))
    r))


(defn print-lines
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