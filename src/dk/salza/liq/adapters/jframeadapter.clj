(ns dk.salza.liq.adapters.jframeadapter
  (:require [dk.salza.liq.tools.util :as util]
            [dk.salza.liq.editor :as editor]
            [dk.salza.liq.window :as window]
            [clojure.string :as str]))


(def pane (atom nil))
(def last-key (atom nil))
(def tmp-keys (atom ""))
(def old-lines (atom {}))
(def updater (ref (future nil)))
(def changes (ref 0))

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
    
    (cond (= (.getModifiers e) java.awt.event.InputEvent/CTRL_MASK)
                             (cond (= code "102") :C-f
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
          (= ch "&") :ampersand
          (= ch "'") :singlequote
          (= ch "(") :parenstart
          (= ch ")") :parenend
          (= ch "*") :asterisk
          (= ch "+") :plus
          (= ch ",") :comma
          (= ch "-") :dash
          (= ch ".") :dot
          (= ch "/") :slash
          (= ch ":") :colon
          (= ch ";") :semicolon
          (= ch "<") :lt
          (= ch "=") :equal
          (= ch ">") :gt
          (= ch "?") :question
          (= ch "@") :at
          (= ch "[") :bracketstart
          (= ch "]") :bracketend
          (= ch "^") :hat
          (= ch "{") :bracesstart
          (= ch "_") :underscore
          (= ch "\\") :backslash
          (= ch "|") :pipe
          (= ch "}") :bracesend
          (= ch "~") :tilde
          (= ch "¤") :curren
          (= ch "´") :backtick
          (= ch "Å") :caa
          (= ch "Æ") :cae
          (= ch "Ø") :coe
          (= ch "å") :aa
          (= ch "æ") :ae
          (= ch "ø") :oe
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

;textArea.setFont(new Font("monospaced", Font.PLAIN, 12));
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

(defn update-gui
  []
  (let [windows (reverse (editor/get-windows))
        buffers (map #(editor/get-buffer (window/get-buffername %)) windows)
        lineslist (doall (pmap #(window/render %1 %2) windows buffers))]
        (jframeprint-lines lineslist)))

(defn request-update-gui
  []
  (when (future-done? @updater)
    (dosync (ref-set updater
            (future
              (loop [ch @changes]
                (update-gui)
                (when (not= ch @changes) (recur @changes))))))))

(defn init
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
                                         (dosync (alter changes inc))
                                         (request-update-gui))
                     ))))
  (doto (javax.swing.JFrame. "λiquid")
    (.setDefaultCloseOperation (javax.swing.JFrame/EXIT_ON_CLOSE))
    (.add @pane)
    (.setSize 1200 800)
    (.show)))

(defn jframequit
  []
  (System/exit 0))
