(ns dk.salza.liq.adapters.recorder
  (:require [dk.salza.liq.tools.util :as util]
            [dk.salza.liq.renderer :as renderer]
            [dk.salza.liq.editor :as editor]
            [dk.salza.liq.window :as window]
            [clojure.string :as str]))

(def old-lines (atom {}))
(def filename (atom ""))
(def changelist (atom ""))
(def dimensions (atom {:rows 40 :columns 80}))

(defn row
  [window r]
  (str "<div class=\"row\" id=\"w" window "-r" r "\"></div>"))

(defn html
  [changes]
  (str
  "<html><head>\n
     <meta charset=\"utf-8\"/>
     <style>body {
        //background-color: #181818;
        //background-color: #080808;
        margin: 0;
        //margin-top: 50;
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

      span.bghl {
        background-color: yellow;
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
        //margin: 0px auto;
        border-color: #000000;
        background-color: #181818;
      }
</style>
<script type=\"text/javascript\">

   let content = [" changes "];


   function init() {
     function updateLine(line) {
       var parts = line.match(/(w\\d+-r\\d+):(.*)/);
       if (document.getElementById(parts[1]).innerHTML != parts[2]) {
         document.getElementById(parts[1]).innerHTML = parts[2];
       }
     }

     function handleNext() {
       if (content.length > 0) {
         var group = content.shift();
         group.forEach(updateLine);
         setTimeout(handleNext, 100);
       }
     }
   handleNext();
   }

</script></head>

<body onload=\"init();\"><table><tr>"
       "<td>" (str/join "\n" (map #(row 0 %) (range 1 (inc (@dimensions :rows))))) "</td>"
       "<td>" (str/join "\n" (map #(row 1 %) (range 1 (inc (@dimensions :rows))))) "</td>"
       "</tr></table>"
       "<div id=\"tmp\"></div>"
       "</body></html>"))



(defn reset
  []
  (reset! old-lines {}))

(defn convert-line
  [line]
  (let [key (str "'w" (if (= (line :column) 1) "0" "1") "-r" (line :row))
        content (str "<span class=\"bgstatusline\"> </span><span class=\"plain bgplain\">"
                  (str/join (for [c (line :line)] (if (string? c) (str/replace c #"'" "&quot;") (str "</span><span class=\"" (name (c :face)) " bg" (name (c :bgface)) "\">"))))
                  "</span>',")]
    (if (= (@old-lines key) content)
      ""
      (do (swap! old-lines assoc key content)
          (str key ":" content)))))


(defn get-output
  []
  (let [lineslist (renderer/render-screen)]
        (str/join "\n" 
          (filter #(not= "" %) (pmap convert-line (apply concat lineslist))))))


(defn save-file
  []
  (reset! changelist (str @changelist "[" (get-output) "],\n"))
  (spit @filename (html @changelist)))


(defn view-handler
  [key reference old new]
  (remove-watch editor/editor key)
  (future
    (Thread/sleep 200)
    (when (editor/fullupdate?) (reset))
    (save-file)
    (Thread/sleep 200)
    (add-watch editor/updates key view-handler)))

(defn start
  []
  (reset)
  (add-watch editor/updates "recorder" view-handler))

(defn stop
  []
  (remove-watch editor/updates "recorder"))

(defn init
  [rows columns recordfile]
  (reset! filename recordfile)
  (reset! dimensions {:rows rows :columns columns})
  (editor/set-global-key :C-x :1 start)
  (editor/set-global-key :C-x :2 stop))



;  {:init init
;   :rows (fn [] rows)
;   :columns (fn [] columns)
;   :wait-for-input nothing
;   :print-lines nothing
;   :reset nothing
;   :quit nothing})