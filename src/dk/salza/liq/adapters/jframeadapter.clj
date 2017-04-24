(ns dk.salza.liq.adapters.jframeadapter
  (:require [dk.salza.liq.tools.util :as util]
            [dk.salza.liq.editor :as editor]
            [dk.salza.liq.window :as window]
            [clojure.string :as str]))


(def pane (atom nil))
(def old-lines (atom {}))
(def updater (ref (future nil)))

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

(defn set-content
  [id content]
  (let [doc (.getDocument @pane)
        element (.getElement doc id)
        newcontent (convert-line content)]
    (javax.swing.SwingUtilities/invokeLater
      (proxy [Runnable] []
        (run []
          (.setInnerHTML doc element newcontent))))))

;; http://docs.oracle.com/javase/8/docs/api/javax/swing/text/html/HTMLDocument.html
(defn jframeprint-lines
  [lineslist]
  (doseq [line (apply concat lineslist)]
    (let [key (str "w" (if (= (line :column) 1) "0" "1") "-r" (line :row))]
      (when (not= (@old-lines key) line)
        (swap! old-lines assoc key line)
        (set-content key line)))))

(defn view-draw
  []
  (let [windows (reverse (editor/get-windows))
        buffers (map #(editor/get-buffer (window/get-buffername %)) windows)
        lineslist (doall (pmap #(window/render %1 %2) windows buffers))]
        (jframeprint-lines lineslist)))

(defn view-handler
  [key reference old new]
  (remove-watch editor/editor key)
  (when (future-done? @updater)
    (dosync (ref-set updater
            (future
              (loop [u @editor/updates]
                (view-draw)
                (when (not= u @editor/updates)
                  (recur @editor/updates)))))))
  (add-watch editor/updates key view-handler))

(defn model-update
  [input]
  (future
    (editor/handle-input input)))

(defn init
  []
  (reset! pane (doto (javax.swing.JEditorPane.)
                     (.setContentType "text/html")
                     (.setEditable false)
                     (.setFocusTraversalKeysEnabled false)
                     (.setDoubleBuffered true)
                     (.setText (html))
                     (.addKeyListener (proxy [java.awt.event.KeyListener] []
                       (keyPressed [e] (model-update (event2keyword e)))
                       (keyReleased [e] (do))
                       (keyTyped [e] (do))))))
  (doto (javax.swing.JFrame. "λiquid")
    (.setDefaultCloseOperation (javax.swing.JFrame/EXIT_ON_CLOSE))
    (.add @pane)
    (.setSize 1200 800)
    (.show))
  (add-watch editor/updates "jframe" view-handler))

(defn jframequit
  []
  (System/exit 0))