(ns dk.salza.liq.adapters.jframeadapter
  (:require [dk.salza.liq.tools.util :as util]
            [dk.salza.liq.renderer :as renderer]
            [dk.salza.liq.editor :as editor]
            [dk.salza.liq.window :as window]
            [dk.salza.liq.logging :as logging]
            [clojure.string :as str]
            [clojure.java.io :as io])
  (:import [java.awt Font Color GraphicsEnvironment Dimension GraphicsDevice Window]
           [java.awt.event InputEvent KeyListener ComponentListener WindowAdapter]
           [java.awt.image BufferedImage]
           [javax.swing JFrame ImageIcon JPanel]))


(def ^:private frame (atom nil))
(def ^:private panel (atom nil))
(def ^:private pane (atom nil))
(def ^:private old-lines (atom {}))
(def ^:private updater (atom (future nil)))
(def ^:private rows (atom 46))
(def ^:private columns (atom 160))

(defn- is-windows
  []
  (re-matches #"(?i)win.*" (System/getProperty "os.name")))

(defn- hexcolor
  [h]
  (Color/decode (str "0x" h)))

(def ^:private colors
  {:plain (hexcolor "e4e4ef")
   :type1 (hexcolor "ffdd33")
   :type2 (hexcolor "95a99f")
   :type3 (hexcolor "ffdd33")
   :green (hexcolor "73c936")
   :yellow (hexcolor "ffdd33")
   :red (hexcolor "ff0000")
   :comment (hexcolor "cc8c3c")
   :string (hexcolor "73c936")
   :stringst (hexcolor "73c936")
   :default (hexcolor "aaaaaa")})

(def ^:private bgcolors
  {:plain (hexcolor "181818")
   :cursor0 (hexcolor "181818")
   :cursor1 (hexcolor "336633")
   :cursor2 (hexcolor "0000cc")
   :hl (hexcolor "ffff00")
   :selection (hexcolor "ff0000")
   :statusline (hexcolor "000000")
   :default (hexcolor "333333")})

(def fontsize (atom 14))

(def font (atom nil))

(defn list-fonts
  "Output a list of all fonts"
  []
  (str/join "\n"
    (.getAvailableFontFamilyNames
      (GraphicsEnvironment/getLocalGraphicsEnvironment))))

(defn- update-font
  ([f]
    (reset! font
      (let [allfonts (into #{} (.getAvailableFontFamilyNames (GraphicsEnvironment/getLocalGraphicsEnvironment)))
            myfonts (concat
                      (when f (list f))
                        (list
                        "Lucida Sans Typewriter"
                        "Consolas"
                        "monospaced"
                        "Inconsolata"
                        "DejaVu Sans Mono"
                        "Ubuntu Mono"
                        "Courier"
                      ))]
        (Font. (some allfonts myfonts) Font/PLAIN @fontsize))))
  ([] (update-font nil)))

(def ^:private fontwidth (atom 8))
(def ^:private fontheight (atom 18))

(defn- view-draw
  []
  (.repaint @panel))

(defn- view-handler
  [key reference old new]
  (remove-watch editor/editor key)
  (when (future-done? @updater)
    (reset! updater
      (future
        (loop [u @editor/updates]
          (view-draw)
          (when (not= u @editor/updates)
            (recur @editor/updates))))))
  (add-watch editor/updates key view-handler))

(defn- toggle-fullscreen
  []
  (let [ge (GraphicsEnvironment/getLocalGraphicsEnvironment)
        screen (.getDefaultScreenDevice ge)]
      (when (.isFullScreenSupported screen)
        (if (.getFullScreenWindow screen)
          (.setFullScreenWindow screen nil)
          (.setFullScreenWindow screen @frame)))))

(defn- model-update
  [input]
  (logging/log "INPUT" input)
  (if (= input "f11")
    (toggle-fullscreen)
    (future
      (editor/handle-input input))))

(defn- draw-char
  [g char row col color bgcolor]
  (let [w @fontwidth
        h @fontheight]
    (.setColor g (bgcolors bgcolor))
    (.fillRect g (* col w) (* (- row 1) h) w h)
    (.setColor g (colors color))
    (.drawString g char (* col w) (- (* row h) (quot @fontsize 4) 1))))

(defn- draw
  [g]
  (let [lineslist (renderer/render-screen)]
    (.setFont g @font)
    (when (editor/fullupdate?)
      (reset! old-lines {})
      (.setColor g (bgcolors :plain))
      (.fillRect g 0 0 (.getWidth @panel) (.getHeight @panel)))
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
                    (draw-char g " " row (+ column offset) :plain :plain)
                    (recur (rest c) (+ offset 1)))))
              (draw-char g " " row (- column 1) :plain :statusline)
              (loop [c content offset 0 color :plain bgcolor :plain]
                (when (not-empty c)
                  (let [ch (first c)]

                    (if (string? ch)
                      (do
                        (draw-char g ch row (+ column offset) color bgcolor)
                        (recur (rest c) (+ offset 1) color bgcolor))
                      (let [nextcolor (or (ch :face) color)
                            nextbgcolor (or (ch :bgface) bgcolor)]
                        (draw-char g (or (ch :char) "…") row (+ column offset) nextcolor nextbgcolor)
                        (recur (rest c) (+ offset 1) nextcolor nextbgcolor))))))))
        (swap! old-lines assoc key content)))))

(defn- handle-keydown
  [e]
  (let [code (.getExtendedKeyCode e)
        raw (int (.getKeyChar e))
        ctrl (when (.isControlDown e) "C-")
        alt (when (or (.isAltDown e) (.isMetaDown e)) "M-")
        shift (when (.isShiftDown e) "S-")
        key (cond (<= 112 code 123) (str shift ctrl alt "f" (- code 111))
                  (= code 135) "~"
                  (= code 129) "|"
                  (> raw 40000) (str shift (cond
                                  (= code 36) "home"
                                  (= code 35) "end"
                                  (= code 34) "pgdn"
                                  (= code 33) "pgup"
                                  (= code 37) "left"
                                  (= code 39) "right"
                                  (= code 38) "up"
                                  (= code 40) "down"))
                  (and ctrl alt (= raw 36)) "$"
                  (and ctrl alt (= raw 64)) "@"
                  (and ctrl alt (= raw 91)) "["
                  (and ctrl alt (= raw 92)) "\\" ;"
                  (and ctrl alt (= raw 93)) "]"
                  (and ctrl alt (= raw 123)) "{"
                  (and ctrl alt (= raw 125)) "}"
                  (and ctrl (= raw 32)) "C- "
                  ctrl (str ctrl alt (char (+ raw 96)))
                  alt (str ctrl alt (char raw))
                  (= raw 127) "delete"
                  (>= raw 32) (str (char raw))
                  (= raw 8) "backspace"
                  (= raw 9) "\t"
                  (= raw 10) "\n"
                  true (str (char raw)))]
    (when key (model-update key))))

(defn redraw-frame
  []
  (let [tmpg (.getGraphics (BufferedImage. 40 40 BufferedImage/TYPE_INT_RGB))]
    (.setFont tmpg @font)
    (reset! fontwidth (.stringWidth (.getFontMetrics tmpg) "M"))
    (reset! fontheight (+ (.getHeight (.getFontMetrics tmpg)) 1))
    (let [wndcount (count (editor/get-windows))
          buffername (editor/get-name)]
      (editor/set-frame-dimensions (quot (.getHeight @panel) @fontheight) (quot (.getWidth @panel) @fontwidth))
      (when (= wndcount 2)
        (editor/split-window-right 0.22)
        (editor/switch-to-buffer "-prompt-")
        (editor/other-window))
      (editor/switch-to-buffer buffername)
      (editor/request-fullupdate)
      (editor/updated)
      (view-draw)
      (Thread/sleep 20)
      (editor/request-fullupdate)
      (editor/updated)
      (view-draw))))

(defn set-font
  [font-name font-size]
  (reset! fontsize font-size)
  (update-font font-name)
  (redraw-frame))

(defn init
  [rowcount columncount & {:keys [font-name font-size]}]
  (when font-size (reset! fontsize font-size))
  (update-font font-name)
  (let [icon (io/resource "liquid.png")
        tmpg (.getGraphics (BufferedImage. 40 40 BufferedImage/TYPE_INT_RGB))]
    (.setFont tmpg @font)
    (reset! fontwidth (.stringWidth (.getFontMetrics tmpg) "M"))
    (reset! fontheight (+ (.getHeight (.getFontMetrics tmpg)) 1))
    (reset! rows rowcount)
    (reset! columns columncount)

    (reset! panel
      (proxy [JPanel] []
        (paintComponent [g]
          (draw g))))
    (.setFocusTraversalKeysEnabled @panel false)
    (.setPreferredSize @panel (Dimension. (* @columns @fontwidth) (* @rows @fontheight)))
    (.setDoubleBuffered @panel true)

    (reset! frame (JFrame. "λiquid"))
    (.setDefaultCloseOperation @frame (JFrame/EXIT_ON_CLOSE))
    (.setContentPane @frame @panel)
    (.setBackground @frame (bgcolors :plain))
    (.setFocusTraversalKeysEnabled @frame false)
    (.addKeyListener @frame
      (proxy [KeyListener] []
        (keyPressed [e] (handle-keydown e))
        (keyReleased [e] (do))
        (keyTyped [e] (do))))
    (.addWindowListener @frame
      (proxy [WindowAdapter] []
        (windowActivated [e] (editor/request-fullupdate) (view-draw))))
    (.setIconImage @frame (when icon (.getImage (ImageIcon. icon))))
    (.pack @frame)
    (.show @frame)
    (add-watch editor/updates "jframe" view-handler)
    (editor/request-fullupdate)
    (editor/updated)
    (view-draw)
    (Thread/sleep 400)
    (editor/request-fullupdate)
    (editor/updated)
    (view-draw)
    (.addComponentListener @frame
      (proxy [ComponentListener] []
        (componentShown [c])
        (componentMoved [c])
        (componentHidden [c])
        (componentResized [c] (redraw-frame))))))

(defn jframequit
  []
  (System/exit 0))