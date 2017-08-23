(ns dk.salza.liq.adapters.jframeadapter
  (:require [dk.salza.liq.tools.util :as util]
            [dk.salza.liq.renderer :as renderer]
            [dk.salza.liq.editor :as editor]
            [dk.salza.liq.window :as window]
            [dk.salza.liq.logging :as logging]
            [clojure.string :as str]))


(def frame (atom nil))
(def pane (atom nil))
(def old-lines (atom {}))
(def updater (ref (future nil)))
(def rows (atom 46))
(def columns (atom 160))

(defn is-windows
  []
  (re-matches #"(?i)win.*" (System/getProperty "os.name")))

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
               :hl "ffff00"
               :selection "ff0000"
               :statusline "000000"
               :default "333333"})


(defn event2keyword
  [e]
  (let [ch (str (.getKeyChar e))
        code (re-find #"(?<=primaryLevelUnicode=)\d+" (.paramString e))
        rawcode (re-find #"(?<=rawCode=)\d+" (.paramString e))]
    ;(println "----" ch)
    (logging/log
      "KEYCHAR:   " ch "\n"
      "CTRL:      " (.isControlDown e) "\n"
      "CODE:      " code "\n"
      "RAW CODE:  " rawcode "\n"
      "PARAM STR: " (.paramString e)) ; ,primaryLevelUnicode=108
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
          (= (.getModifiers e) java.awt.event.InputEvent/META_MASK)
                                   (cond (= code "10") :M-enter
                                   (= code "97") :M-a
                                   (= code "98") :M-b
                                   :else :unknown)
          (re-matches #"[a-zA-Z0-9]" ch) (keyword ch)
          (= rawcode "110") :home
          (= rawcode "111") :up
          (= rawcode "115") :end
          (= rawcode "116") :down
          (= rawcode "113") :left
          (= rawcode "114") :right
          (= rawcode "117") :pagedown
          (= rawcode "112") :pageup
          (= rawcode "67") :f1
          (= rawcode "68") :f2
          (= rawcode "69") :f3
          (= rawcode "70") :f4
          (= rawcode "71") :f5
          (= rawcode "72") :f6
          (= rawcode "73") :f7
          (= rawcode "74") :f8
          (= rawcode "75") :f9
          (= rawcode "76") :f10
          (= rawcode "95") :f11
          (= rawcode "96") :f12
          (= code "27") :esc
          (= code "8") :backspace
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
          :else (keyword ch))))

(defn row
  [window r]
  (str "<p id=\"w" window "-r" r "\"></p>"))

(defn html
  []
  (str "<html>" ;<head><style>" style "</style></head>"
       "<body bgcolor=\"" (bgcolors :plain) "\">"
       "<table bgcolor=\"" (bgcolors :plain) "\"><tr>"
       "<td>" (str/join "" (map #(row 0 %) (range 1 (inc @rows)))) "</td>"
       "<td>" (str/join "" (map #(row 1 %) (range 1 (inc @rows)))) "</td>"
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
  (let [font (if (is-windows) "Consolas" "monospaced")]
    (str "<font face=\"" font "\" color=\"000000\" bgcolor=\"000000\">-</font><font face=\"" font "\" color=\"" (colors :plain) "\" bgcolor=\"" (bgcolors :plain) "\">"
      (str/join (for [c (line :line)] (if (string? c) (escape c) (str "</span><font face=\"" font "\" color=\"" (colors (c :face)) "\" bgcolor=\""   (bgcolors (c :bgface)) "\">"))))
    "</font>"
  )))

(defn set-content
  [id content]
  (let [doc (.getDocument @pane)
        element (.getElement doc id)
        newcontent (convert-line content)]
    (javax.swing.SwingUtilities/invokeLater
      (proxy [Runnable] []
        (run []
          (.setInnerHTML doc element newcontent)
          (.pack @frame)
          )))))

;; http://docs.oracle.com/javase/8/docs/api/javax/swing/text/html/HTMLDocument.html
(defn jframeprint-lines
  [lineslist]
  ;(println (pr-str (first (second lineslist))))
  ;(println (convert-line (first (second lineslist))))
  (doseq [line (apply concat lineslist)]
    (let [key (str "w" (if (= (line :column) 1) "0" "1") "-r" (line :row))]
      (when (not= (@old-lines key) line)
        (swap! old-lines assoc key line)
        (set-content key line)))))

(defn view-draw
  []
  (jframeprint-lines (renderer/render-screen))
  ;(.setSize @frame (.getSize @pane))
  )

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
  (logging/log "INPUT" input)
  (future
    (editor/handle-input input)))

(defn init
  [rowcount columncount]
  (let [icon (clojure.java.io/resource "liquid.png")]
    (reset! rows rowcount)
    (reset! columns columncount)
    (reset! pane (doto (javax.swing.JEditorPane.)
                       (.setContentType "text/html")
                       (.setEditable false)
                       (.setFocusTraversalKeysEnabled false)
                       (.setDoubleBuffered true)
                       (.setText (html))
                       (.setMargin (java.awt.Insets. 0 0 0 0))
                       (.addKeyListener (proxy [java.awt.event.KeyListener] []
                         (keyPressed [e] (model-update (event2keyword e)))
                         (keyReleased [e] (do))
                         (keyTyped [e] (do))))))
    (reset! frame 
      (doto (javax.swing.JFrame. "λiquid")
        (.setDefaultCloseOperation (javax.swing.JFrame/EXIT_ON_CLOSE))
        (.setLayout (java.awt.FlowLayout. java.awt.FlowLayout/CENTER 0 0))
        (.add @pane)
        ;(.setSize 1200 800)
        ;(.pack)
        (.setIconImage (when icon (.getImage (javax.swing.ImageIcon. icon))))
        (.show)))
    (add-watch editor/updates "jframe" view-handler)
    (editor/updated)))

(defn jframequit
  []
  (System/exit 0))