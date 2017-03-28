(ns dk.salza.liq.adapters.jframeadapter
  (:require [dk.salza.liq.tools.util :as util]
            [dk.salza.liq.editor :as editor]
            [dk.salza.liq.window :as window]
            [clojure.string :as str]))


(def pane (atom nil))
(def last-key (atom nil))
(def tmp-keys (atom ""))
(def old-lines (atom {}))
; (def g (atom nil))
; 
; (def crow (atom 1))
; (def ccolumn (atom 1))
; (def ccolor (atom (java.awt.Color. 255 100 0)))
; (def cbgcolor (atom (java.awt.Color. 20 20 20)))

(def colors {:plain "e4e4ef"
             :type1 "ffdd33"
             :type2 "95a99f"
             :type3 "ffdd33"
             :comment "cc8c3c"
             :string "73c936"
             :default "aaaaaa"})

(def bgcolors {:plain "181818"
               :cursor1 "00ff00"
               :cursor2 "0000ff"
               :selection "ff0000"
               :statusline "000000"
               :default "333333"})


(defn event2keyword
  [e]
  (let [ch (str (.getKeyChar e))
        code (re-find #"(?<=primaryLevelUnicode=)\d+" (.paramString e))]
    ;(println "----" ch)
    ;(println "KEYCHAR:   " ch)
    ;(println "CTRL:      " (.isControlDown e))
    ;(println "CODE:      " code)
    ;(println "PARAM STR: " (.paramString e)); ,primaryLevelUnicode=108,
    (cond (.isControlDown e) (cond (= code "102") :C-f
                                   (= code "103") :C-g
                                   (= code "106") :C-j
                                   (= code "107") :C-k
                                   (= code "108") :C-l
                                   (= code "111") :C-o
                                   (= code "113") :C-q
                                   (= code "115") :C-s
                                   (= code "116") :C-t
                                   (= code "32") :C-space
                                   :else :unknown)
          (= ch "\t") :tab
          (= ch " ") :space
          (= ch "\n") :enter
          (= ch "!") :exclamation
          (= ch "\"") :quote
          (= ch "#") :hash
          (= ch "$") :dollar
          (= ch "%") :percent
          :else (keyword (str (.getKeyChar e))))))

(defn jframerows
  []
  46)

(defn jframecolumns
  []
  160)

(defn row
  [window r]
  (str "<p id=\"w" window "-r" r "\"></p>"))

(defn html
  []
  (str "<html>" ;<head><style>" style "</style></head>"
       "<body bgcolor=\"" (bgcolors :plain) "\">"
       "<table bgcolor=\"" (bgcolors :plain) "\"><tr>"
       "<td>" (str/join "" (map #(row 0 %) (range 1 (inc (jframerows))))) "</td>"
       "<td>" (str/join "" (map #(row 1 %) (range 1 (inc (jframerows))))) "</td>"
       "</tr></table>"
       "<div id=\"tmp\"></div>"
       "</body></html>"))


(defn escape
  [c]
  (cond (= c " ") "&nbsp;"
        :else c))


(defn convert-line
  [line]
  (str "<font face=\"Inconsolata\" color=\"000000\" bgcolor=\"000000\">-</font><font face=\"Inconsolata\" color=\"" (colors :plain) "\" bgcolor=\"" (bgcolors :plain) "\">"
    (str/join (for [c (line :line)] (if (string? c) (escape c) (str "</span><font face=\"Inconsolata\" color=\"" (colors (c :face)) "\" bgcolor=\"" (bgcolors (c :bgface)) "\">"))))
    "</font>"
  ))




(defn jframewait-for-input
  []
  (while (not @last-key)
    (Thread/sleep 5))
  (let [res @last-key]
    ;(println "---")
    (reset! last-key nil)
    (swap! tmp-keys #(str % "-" res))
    res))

(defn set-content
  [id content]
  (.setInnerHTML (.getDocument @pane) (.getElement (.getDocument @pane) id) content)) 

;; http://docs.oracle.com/javase/8/docs/api/javax/swing/text/html/HTMLDocument.html
(defn jframeprint-lines
  [lineslist]
  ;(println "Printing lines..")
  (let [text (str "<html>"
               "  <body bgcolor=\"" (bgcolors :plain) "\">"
               "<table><tr>"
               (str/join "\n" (for [lines lineslist] (str "<td>" (str/join "<br />\n" (map convert-line lines)) "</td>")))
               "</tr></table>"
               "<div id=\"tmp\">" "</div>"
               "</body></html>")]
    ;(.setText @pane text)
    (doseq [line (apply concat lineslist)]
      (let [key (str "w" (if (= (line :column) 1) "0" "1") "-r" (line :row))
            content (convert-line line)]
        (when (not= (@old-lines key) content)
          (swap! old-lines assoc key content)
          (set-content key (convert-line line)))))

    ;(set-content "tmp" (str "<font color=\"888888\">abc " (System/currentTimeMillis) "</font>"))
    ;(println (.getText @pane))
    ;(.setCaretPosition @pane (count text))
    ;(.repaint @pane)
    ))

(defn update-output
  []
  (let [windows (reverse (editor/get-windows))
        buffers (map #(editor/get-buffer (window/get-buffername %)) windows)
        lineslist (doall (pmap #(window/render %1 %2) windows buffers))]
        (jframeprint-lines lineslist)))

(defn jframeinit
  []
  (reset! pane (doto (javax.swing.JEditorPane.)
                     ;(javax.swing.JTextPane.)
                     (.setContentType "text/html")
                     (.setEditable false)
                     (.setFocusTraversalKeysEnabled false)
                     (.setDoubleBuffered true)
                     (.setText (html))
                     (.addKeyListener (proxy [java.awt.event.KeyListener] []
                       (keyPressed [e] (do))
                       (keyReleased [e] (do))
                       ;(keyTyped [e] (reset! last-key (event2keyword e)))
                       (keyTyped [e] (do (editor/handle-input (event2keyword e)))
                                         (update-output))
                     ))))
  (doto (javax.swing.JFrame. "λiquid")
    (.setDefaultCloseOperation (javax.swing.JFrame/EXIT_ON_CLOSE))
    (.add @pane)
    (.setSize 1200 800)
    (.show)))

(defn jframequit
  []
  (System/exit 0))

(def adapter {:init jframeinit
              :rows jframerows
              :columns jframecolumns
              :wait-for-input jframewait-for-input
              :print-lines jframeprint-lines
              :reset (fn [] (do))
              :quit jframequit})


; (defn get-output
;   []
;   (let [windows (reverse (editor/get-windows))
;         buffers (map #(editor/get-buffer (window/get-buffername %)) windows)
;         lineslist (doall (map #(window/render %1 %2) windows buffers))]
;     (str "<html><head><style>" style "</style><script type=\"text/javascript\">" javascript "</script></head>"
;          "  <body onload=\"init();\">"
;          "<table>"
;          (str/join "\n" (for [lines lineslist] (str "<td>" (map convert-line lines) "</td>")) 
;          "</tr></table>"
;          "<div id=\"tmp\"></div>"
;          "</body></html>"))))

; (defn jframeprint-lines2
;   [lines]
;   (str "<html><head><style>" style "</style><script type=\"text/javascript\">" javascript "</script></head>"
;        "  <body onload=\"init();\">"
;        "<table>"
;        (str/join "\n" (str "<td>" (map convert-line lines) "</td>")) 
;        "</tr></table>"
;        "<div id=\"tmp\"></div>"
;        "</body></html>"))
  

; (defn jframeinit
;   []
;   (let [html "<html><body><font color=\"red\" bgcolor=\"blue\">RED</font> not<br></body></html>"
;         editorpane (doto (javax.swing.JEditorPane.)
;                      (.setContentType "text/html")
;                      (.setEditable false)
;                     ;(.setFocusTraversalKeysEnabled false)
;                      (.setText html)
;                      (.addKeyListener (proxy [java.awt.event.KeyListener] []
;                        (keyPressed [e] (do))
;                        (keyReleased [e] (do))
;                        (keyTyped [e] (reset! last-key (event2keyword e))))))
;         ;panel (doto (javax.swing.JPanel.)
;         ;        ;(.setFocusTraversalKeysEnabled false)
;         ;        (.addKeyListener (proxy [java.awt.event.KeyListener] []
;         ;           (keyPressed [e] (println "a"))
;         ;           (keyReleased [e] (println "b"))
;         ;           (keyTyped [e] (reset! last-key (event2keyword e))))))
;         ;tmplabel (javax.swing.JLabel. "<html><body><font color=\"red\" bgcolor=\"blue\">RED</font> not</body></html>")
;                 ]
;     (doto (javax.swing.JFrame. "λiquid")
;       (.add editorpane)
;       ;(.add panel)
;       ;(.add tmplabel)
;       (.setSize 800 600)
;       (.show))))
    ;(while (nil? (.getGraphics panel))
    ;  (Thread/sleep 50))
    ;(.requestFocus panel)
    ;(reset! g (.getGraphics panel))))
    ;(reset! pane editorpane)))


; (defn plot
;   [letter]
;   (let [h 16
;         w 8
;         x (* (- @crow 1) h)
;         y (* (- @ccolumn 1) w)]
;     (.setColor @g @cbgcolor)
;     (.fillRect @g y x w h)
;     (.setColor @g @ccolor)
;     (.drawString @g letter y (+ x h -3))
;     (swap! ccolumn inc)))

; (def old-lines (atom {}))


; (defn jframeprint-lines
;   [lines]
;   (.setFont @g (java.awt.Font. "monospaced" java.awt.Font/PLAIN 12)); 
;   (doseq [line lines]
;     (let [row (line :row)
;           column (line :column)
;           content (line :line)
;           key (str "k" row "-" column)
;           oldcontent (@old-lines key)
;           gr @g] 
;     (when (not= oldcontent content)
;       (let [diff (max 1 (- (count (filter #(and (string? %) (not= % "")) oldcontent))
;                            (count (filter #(and (string? %) (not= % "")) content))))
;             padding (format (str "%" diff "s") " ")]
;         (reset! crow row)
;         (reset! ccolumn column)
;         (doseq [ch (line :line)]
;           (if (string? ch)
;             (if (= ch "\t") (plot "-") (plot ch))
;             ;(do
;             ;  (cond (= (ch :face) :string) (print "\033[38;5;131m")
;             ;        (= (ch :face) :comment) (print "\033[38;5;105m")
;             ;        (= (ch :face) :type1) (print "\033[38;5;11m") ; defn
;             ;        (= (ch :face) :type2) (print "\033[38;5;40m") ; function
;             ;        (= (ch :face) :type3) (print "\033[38;5;117m") ; keyword
;             ;        :else (print "\033[0;37m"))
;             ;  (cond (= (ch :bgface) :cursor1) (print "\033[42m")
;             ;        (= (ch :bgface) :cursor2) (print "\033[44m")
;             ;        (= (ch :bgface) :selection) (print "\033[48;5;52m")
;             ;        (= (ch :bgface) :statusline) (print "\033[48;5;235m")
;             ;        :else (print "\033[49m"))
;             ;)))
;         ;(if (= row (count lines))
;         ;  (print (str "  " padding))
;         ;  (print (str "\033[0;37m\033[49m" padding))))
;       (swap! old-lines assoc key content))
;     )))))
; 
; 
; 
;   ;; (doseq [[x y] (map vector (range 3) (range 3))] (println x "," y))
; 
; ;  (doseq [line lines]
; ;    (let [row (line :row)
; ;          column (line :column)
; ;          content (line :line)] 
; ;        (doseq [ch (line :line)]
; 
; ;  (draw-letter @g "a")
; ;  (draw-letter @g "b")
; ;  (draw-letter @g "c")
; ;  (draw-letter @g 2 20 "a" (java.awt.Color. 255 100 0) (java.awt.Color. 0 0 0))
; ;  (draw-letter @g 2 22 "b" (java.awt.Color. 255 100 0) (java.awt.Color. 0 0 0))
; ;  (draw-letter @g 2 24 "c" (java.awt.Color. 255 100 0) (java.awt.Color. 0 0 0))
; ;  (draw-letter @g 3 20 "d" (java.awt.Color. 155 0 0) (java.awt.Color. 0 0 0))
; ;  (draw-letter @g 3 21 "e" (java.awt.Color. 155 100 0) (java.awt.Color. 0 0 0))
;   )
;   ;(.setText @pane (str/join "<br>\n" (doall (map #(apply str (% :line)) lines)))))
;   ;(.setText @pane (str "abc<b>test</b>" @tmp-keys)))