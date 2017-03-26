(ns dk.salza.liq.adapters.webadapter
  (:require [dk.salza.liq.tools.util :as util]
            [dk.salza.liq.keys :as keys]
            [dk.salza.liq.editor :as editor]
            [dk.salza.liq.window :as window]
            [clojure.string :as str])
  (:import [java.io OutputStream]
           [java.net InetSocketAddress]
           [com.sun.net.httpserver HttpExchange HttpHandler HttpServer]))

(comment "

Brainsstorm
-----------

1. m pressed in browser
2. Ajax call to liquid with /key/m (or /key/77)
3. Deliver :m to input promise  
4. wait-for-input reads input promise
5. (editor executes)
6. print-lines delivers delta lines to output promise
7. Ajax call returns output promise



Brainstorm - editor approach (Maybe not)
1. m pressed in browser
2. Ajax call to liquid with /key/m (or /key/77)
3. call editor/handle-input :m



")

(def server (atom nil))
;(def output (atom (promise)))
;; (def outputchanges (clojure.lang.PersistentQueue/EMPTY))
(def input (atom (promise)))
(def dimensions (atom {:rows 40 :columns 80}))
(def autoupdate (atom false))


(def style
     "body {
        //background-color: #181818;
        background-color: #080808;
        margin: 0;
        margin-top: 50;
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
     function updateLine(line) {
       var parts = line.match(/(w\\d+-r\\d+):(.*)/);
       if (document.getElementById(parts[1]).innerHTML != parts[2]) {
         document.getElementById(parts[1]).innerHTML = parts[2];
       }
     }

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

     function updategui(evt) {
       if (evt) {
         evt.preventDefault();
         evt.stopPropagation();
         //document.getElementById(\"tmp\").innerHTML = evt.which + ' ' + evt.ctrlKey + ' ' + evt.altKey + ' ' + evt.shiftKey + ' ' + evt.key;
       }
       xhttp.abort();
       xhttp.onreadystatechange = function() {
         if (this.readyState == 4 && this.status == 200) {
           this.responseText.split('\\n').forEach(updateLine)
           //document.getElementById(\"w0-r4\").innerHTML = this.responseText;
         }
       };
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
       ;"a "
       ;(System/currentTimeMillis)
       "<table><tr>"
       "<td>" (str/join "\n" (map #(row 0 %) (range 1 (inc (@dimensions :rows))))) "</td>"
       "<td>" (str/join "\n" (map #(row 1 %) (range 1 (inc (@dimensions :rows))))) "</td>"
       "</tr></table>"
       "<div id=\"tmp\"></div>"
       "</body></html>"))

; java -cp /home/molu/resources/clojure-1.8.0.jar:/home/molu/m/lib2  clojure.main -m dk.salza.liqscratch.server
; (slurp "http://localhost:8520")
; http://www.rgagnon.com/javadetails/java-have-a-simple-http-server.html
; http://unixpapa.com/js/key.html

;(defn get-output
;  []
;  (let [out @@output]
;    (println out)
;    (reset! output (promise))
;    (str/join "<br />" out)))

;(let [path "/key/74"
;      num (Integer/parseInt (re-find #"(?<=/key/)\d+" path))]
;  (if num (keyword (str (char (+ num 32))))))

(def old-lines (atom {}))

(defn convert-line
  [line]
  (let [key (str "w" (if (= (line :column) 1) "0" "1") "-r" (line :row))
        content (str "<span class=\"bgstatusline\"> </span><span class=\"plain bgplain\">"
                  (str/join (for [c (line :line)] (if (string? c) c (str "</span><span class=\"" (name (c :face)) " bg" (name (c :bgface)) "\">"))))
                  "</span>")]
    (str key ":" content)))
    ;(if (= (@old-lines key) content)
    ;  ""
    ;  (do (swap! old-lines assoc key content)
    ;      (str key ":" content)))))


(defn get-output1
  []
  (let [windows (reverse (editor/get-windows))
        buffers (map #(editor/get-buffer (window/get-buffername %)) windows)
        lineslist (doall (map #(window/render %1 %2) windows buffers))]
        ;(spit "/tmp/lines.txt" (pr-str lineslist)) 
        ;k(when (editor/check-full-gui-update)
        ;  ((@adapter :reset)))
        ;(doseq [lines lineslist]
        ;  ((@adapter :print-lines) lines))))
        (str/join "\n" 
          (filter #(not= "" %) (map convert-line (apply concat lineslist))))))

; (numkey2keyword "1110064") -> C-a
; (nth "abc" 1)
; (int \Z)

(def handler
  (proxy [HttpHandler] []
    (handle
      [exchange]
      (let [path (.getPath (.getRequestURI exchange))
            response (cond (= path "/output") (get-output1)
                           ;(= path "/key/tab") (do (deliver @input :tab) (get-output))
                           (re-find #"^/key/" path) (let [num (re-find #"(?<=/key/).*" path)
                                                          c (keyword num)] 
                                                      (editor/handle-input c)
                                                      (get-output1))
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
  ;(let [r (read-line)]
  ;  :i))

(comment 
; {:column 44, :row 1, :line '( {:face :type1, :bgface :plain} "d" "e" "f" "n" {:face :type2, :bgface :plain} " " "m" "y" "f" "u" "n")}
; -> "w1-r1:<span class=\"type1 bg-plain\">defn</span><span class=\"type2 bg-plain\">&nbsp;myfun</span>"
)


; (convert-line {:column 44, :row 1, :line '({:face :type1, :bgface :plain} "d" "e" "f" "n" {:face :type2, :bgface :plain} " " "m" "y" "f" "u" "n")})

(defn print-lines
  [lines]
  )
  ;(deliver @output
  ;        (map convert-line lines)))


;  (let [simple (clojure.string/join "\n" (map #(clojure.string/join "" (filter string? (% :line))) lines))]
;    (reset! output (str "<pre>" simple "</pre>"))))

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