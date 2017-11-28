(ns dk.salza.liq.adapters.jframeadapter2
  (:require [dk.salza.liq.tools.util :as util]
            [dk.salza.liq.renderer :as renderer]
            [dk.salza.liq.editor :as editor]
            [dk.salza.liq.window :as window]
            [dk.salza.liq.logging :as logging]
            [clojure.string :as str]))


(def frame (atom nil))
(def panel (atom nil))
(def pane (atom nil))
(def old-lines (atom {}))
(def updater (ref (future nil)))
(def rows (atom 46))
(def columns (atom 160))

(defn is-windows
  []
  (re-matches #"(?i)win.*" (System/getProperty "os.name")))

(def colors {:plain (java.awt.Color. 200 200 250) ; "e4e4ef"
             :type1 (java.awt.Color. 255 200 50) ; "ffdd33"
             :type2 (java.awt.Color. 150 200 170) ; "95a99f"
             :type3 (java.awt.Color. 255 200 50) ; "ffdd33"
             :green (java.awt.Color. 150 255 150) ; "73c936"
             :yellow (java.awt.Color. 255 200 50) ; "ffdd33"
             :red (java.awt.Color. 255 0 0) ; "ff0000"
             :comment (java.awt.Color. 200 150 50) ; "cc8c3c"
             :string (java.awt.Color. 150 200 50) ; "73c936"
             :default (java.awt.Color. 200 200 200)}) ; "aaaaaa"

(def bgcolors {:plain (java.awt.Color. 30 30 30) ; "181818"
               :cursor0 (java.awt.Color. 30 30 30) ; "181818"
               :cursor1 (java.awt.Color. 0 255 0) ; "00ff00"
               :cursor2 (java.awt.Color. 0 0 255) ; "0000ff"
               :hl (java.awt.Color. 255 255 0) ; "ffff00"
               :selection (java.awt.Color. 255 0 0) ; "ff0000"
               :statusline (java.awt.Color. 0 0 0) ; "000000"
               :default (java.awt.Color. 20 20 20)}) ; "333333"

(def fontsize (atom 13))

(def font (atom nil))

(defn update-font
  []
  (reset! font
    (let [allfonts (into #{} (.getAvailableFontFamilyNames (java.awt.GraphicsEnvironment/getLocalGraphicsEnvironment)))
          myfonts (list "Inconsolata"
                        "Consolas"
                        "DejaVu Sans Mono"
                        "Ubuntu Mono"
                        "Courier"
                        "monospaced")]
      (java.awt.Font. (some allfonts myfonts) java.awt.Font/PLAIN @fontsize))))


(def fontwidth (atom 8))
(def fontheight (atom 18))

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
                                   (= code "117") :C-u
                                   (= code "118") :C-v
                                   (= code "119") :C-w
                                   (= code "120") :C-x
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

(defn view-draw
  []
  (.repaint @panel))

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

(defn draw-char
  [g char row col color bgcolor]
  (let [w @fontwidth
        h @fontheight]
    (.setColor g (bgcolors bgcolor))
    (.fillRect g (* col w) (* (- row 1) h) w h)
    (.setColor g (colors color))
    (.drawString g char (* col w) (- (* row h) (quot @fontsize 4) 1))))

(defn draw
  [g]
  (let [lineslist (renderer/render-screen)]
    ;(println lineslist)
    (.setFont g @font)
    (when (editor/fullupdate?)
      (reset! old-lines {})
     ; (.setRenderingHints g (java.awt.RenderingHints.
     ;                         java.awt.RenderingHints/KEY_TEXT_ANTIALIASING
     ;                         java.awt.RenderingHints/VALUE_TEXT_ANTIALIAS_ON))

      (.setColor g (bgcolors :plain))
      (.fillRect g 0 0 1600 900))
    (doseq [line (apply concat lineslist)]
      (let [row (line :row)
            column (line :column)
            content (line :line)
            key (str "k" row "-" column)
            oldcontent (@old-lines key)] 
          (when (not= oldcontent content)
            (let [oldcount (count (filter #(and (string? %) (not= % "")) oldcontent))]
              (loop [c oldcontent offset 0]
                (when (not-empty c)
                  (let [ch (first c)]
                    (if (string? ch)
                      (draw-char g " " row (+ column offset) :plain :plain)
                      nil)
                    (recur (rest c) (+ offset (if (string? ch) 1 0))))))
              (draw-char g " " row (- column 1) :plain :statusline)
              (loop [c content offset 0 color :plain bgcolor :plain]
                (when (not-empty c)
                  (let [ch (first c)]
                    (if (string? ch)
                      (do
                        (draw-char g ch row (+ column offset) color bgcolor)
                        (recur (rest c) (+ offset 1) color bgcolor))
                     (recur (rest c) offset (ch :face) (ch :bgface)))
                    )))))
        (swap! old-lines assoc key content)))))

(defn init
  [rowcount columncount & {:keys [font-size]}]
  (when font-size (reset! fontsize font-size))
  (update-font)
  (let [icon (clojure.java.io/resource "liquid.png")
        tmpg (.getGraphics (java.awt.image.BufferedImage. 40 40 java.awt.image.BufferedImage/TYPE_INT_RGB))]
    (.setFont tmpg @font)
    (reset! fontwidth (.stringWidth (.getFontMetrics tmpg) "M"))
    (reset! fontheight (+ (.getHeight (.getFontMetrics tmpg)) 1))
    (reset! rows rowcount)
    (reset! columns columncount)
    (reset! panel (doto
                    (proxy [javax.swing.JPanel] []
                      (paintComponent [g]
                        (draw g)))
                    (.setFocusTraversalKeysEnabled false)
                    (.setPreferredSize (java.awt.Dimension. (* columncount @fontwidth) (* rowcount @fontheight)))
                    (.setDoubleBuffered true)))
    (reset! frame 
      (doto (javax.swing.JFrame. "λiquid")
        (.setDefaultCloseOperation (javax.swing.JFrame/EXIT_ON_CLOSE))
        (.setContentPane @panel)
        (.setBackground (bgcolors :plain))
        (.setFocusTraversalKeysEnabled false)
        (.addKeyListener (proxy [java.awt.event.KeyListener] []
                          (keyPressed [e] (model-update (event2keyword e)))
                          (keyReleased [e] (do))
                          (keyTyped [e] (do))))
        (.setIconImage (when icon (.getImage (javax.swing.ImageIcon. icon))))
        (.pack)
        (.show)))
    (add-watch editor/updates "jframe" view-handler)
    (editor/request-fullupdate)
    (editor/updated)
    (view-draw)
    (Thread/sleep 400)
    (editor/request-fullupdate)
    (editor/updated)
    (view-draw)))

(defn jframequit
  []
  (System/exit 0))