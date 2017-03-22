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


(def style
     "body {
        background-color: #111111;
        margin: 0;
        color: #dddddd
      }

      span.type1 {
        color: yellow;
      }

      span.type2 {
        color: green;
      }

      span.type3 {
        color: blue;
      }

      span.comment {
        color: brown;
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
  "

   var xhttp = new XMLHttpRequest();
   function init() {
     function updateLine(line) {
       var parts = line.match(/(w\\d+-r\\d+):(.*)/);
       document.getElementById(parts[1]).innerHTML = parts[2];
       console.log(parts[1] + '.......' + parts[2] + '\\n');
     }
    
     setInterval(function () {
                   xhttp.abort();
                   xhttp.onreadystatechange = function() {
                     if (this.readyState == 4 && this.status == 200) {
                       this.responseText.split('\\n').forEach(updateLine)
                     }
                   };
                   xhttp.open(\"GET\", \"output\", true);
                   xhttp.send();
                 },200);

     document.onkeydown = function(evt) {
                            evt.preventDefault();
                            evt.stopPropagation();
                            document.getElementById(\"tmp\").innerHTML = evt.which + ' ' + evt.ctrlKey + ' ' + evt.altKey + ' ' + evt.shiftKey;
                            var keynum = 1000000 + 100000 * evt.ctrlKey + 10000 * evt.altKey + 1000 * evt.shiftKey + evt.which
                            xhttp.abort();
                            xhttp.onreadystatechange = function() {
                              if (this.readyState == 4 && this.status == 200) {
                                this.responseText.split('\\n').forEach(updateLine)
                                //document.getElementById(\"w0-r4\").innerHTML = this.responseText;
                              }
                            };
                            xhttp.open(\"GET\", \"key/\" + keynum, true);
                            xhttp.send();
                          }}")


(defn rows
  []
  40)

(defn columns
  []
  120)

(defn row
  [window r]
  (str "<div class=\"row\" id=\"w" window "-r" r "\"></div>"))

(defn html
  []
  (str "<html><head><style>" style "</style><script type=\"text/javascript\">" javascript "</script></head>"
       "  <body onload=\"init();\">"
       ;"a "
       ;(System/currentTimeMillis)
       "<table><tr>"
       "<td>" (str/join "\n" (map #(row 0 %) (range 1 (inc (rows))))) "</td>"
       "<td>" (str/join "\n" (map #(row 1 %) (range 1 (inc (rows))))) "</td>"
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

(defn convert-line
  [line]
  (str "w" (if (= (line :column) 1) "0" "1") "-r" (line :row) ":"
    "<span class=\"plain bgplain\">"
    (str/join (for [c (line :line)] (if (string? c) c (str "</span><span class=\"" (name (c :face)) " bg" (name (c :bgface)) "\">"))))
    "</span>"
  ))

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
          (map convert-line (apply concat lineslist)))))

(defn numkey2keyword
  [num]
  (let [c (if (= (subs num 1 2) "1") "C-" "")
        m (if (= (subs num 2 3) "1") "M-" "")
        s (if (= (subs num 3 4) "1") 0 32)
        n (Integer/parseInt (subs num 4))]
    (cond (and (>= n 65) (<= n 90)) (keyword (str c m (char (+ s n))))
          (= num "1000013") :enter
          (= num "1000190") :dot
          (= num "1100032") :C-space
          (= num "1000008") :backspace
          (= num "1000009") :tab
          :else (str c m (keys/raw2keyword (+ n s))))))

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
                           (re-find #"^/key/" path) (let [num (re-find #"(?<=/key/)\d+" path)
                                                          c (numkey2keyword num)] 
                                                      (editor/handle-input c)
                                                      (get-output1))
                                                      ;(deliver @input c)
                                                      ;(get-output))
                           ;(= path "/key/73") (do (deliver @input :i) (get-output))
                           ;(= path "/key/77") (do (deliver @input :m) (get-output))
                           ;(= path "/key/78") (do (deliver @input :n) "aaaaaaaa")
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
  [& args]
  (reset! server (HttpServer/create (InetSocketAddress. 8520) 0))
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

(def adapter
  {:init init
   :rows rows
   :columns columns
   :wait-for-input wait-for-input
   :print-lines print-lines
   :reset #(do)
   :quit quit})