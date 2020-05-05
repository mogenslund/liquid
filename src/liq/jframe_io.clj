(ns liq.jframe-io
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [liq.buffer :as buffer]
            [liq.util :as util])
  (:import [java.awt Font Color GraphicsEnvironment Dimension GraphicsDevice Window]
           [java.awt.event InputEvent KeyListener ComponentListener WindowAdapter]
           [java.awt.image BufferedImage]
           [javax.swing JFrame ImageIcon JPanel]))

(def ^:private frame (atom nil))
(def ^:private panel (atom nil))
(def ^:private pane (atom nil))

(def ^:private rows (atom 40))
(def ^:private cols (atom 140))

(def ^:private last-buffer (atom nil))

(defn- is-windows
  []
  (re-matches #"(?i)win.*" (System/getProperty "os.name")))

(defn- hexcolor
  [h]
  (Color/decode (str "0x" h)))

(defn convert-colormap
  "Convert hex values in colormap to
  type java.awt.Color."
  [m]
  (reduce (fn [r [k v]] (assoc r k (hexcolor v))) {} m))

(def colors (atom 
  (convert-colormap
    {:plain "e4e4ef"
     :definition "95a99f"
     :special "ffdd33"
     :keyword "ffdd33"
     :green "73c936"
     :yellow "ffdd33"
     :red "ff0000"
     :comment "cc8c3c"
     :string "73c936"
     :stringst "73c936"
     :default "aaaaaa"
     nil "e4e4ef"})))

(def bgcolors (atom
  (convert-colormap
    {:plain "181818"
     :cursor0 "181818"
     :cursor1 "336633"
     :cursor2 "0000cc"
     :hl "ffff00"
     :selection "ff0000"
     :statusline "000000"
     :default "333333"
     nil "333333"})))

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

(def ^:private fontwidth (atom 10))
(def ^:private fontheight (atom 18))

(defn buffer-footprint
  [buf]
  [(buf ::buffer/window) (buf ::buffer/name) (buf ::buffer/file-name)])

(defn- view-draw
  []
  (.repaint @panel))

;(defn- view-handler
;  [key reference old new]
;  (when (future-done? @updater)
;    (reset! updater
;      (future
;        (editor/quit-on-exception
;         (loop [u @editor/updates]
;           (view-draw)
;           (when (not= u @editor/updates)
;             (recur @editor/updates))))))))

(defn- toggle-fullscreen
  []
  (let [ge (GraphicsEnvironment/getLocalGraphicsEnvironment)
        screen (.getDefaultScreenDevice ge)]
      (when (.isFullScreenSupported screen)
        (if (.getFullScreenWindow screen)
          (.setFullScreenWindow screen nil)
          (.setFullScreenWindow screen @frame)))))

;(defn- model-update
;  [input]
;  (logging/log "INPUT" input)
;  (if (= input "f11")
;    (toggle-fullscreen)
;    (future
;      (editor/handle-input input))))

(def char-cache (atom {}))
(defn- draw-char
  [g ch row col color bgcolor]
  (let [w @fontwidth
        h @fontheight
        k (str row "-" col)
        footprint (str ch row col color bgcolor)]
    (when (not= (@char-cache k) footprint)
      (.setColor g (@bgcolors bgcolor))
      (.fillRect g (* col w) (* (- row 1) h) w h)
      (.setColor g (or (@colors color) (@colors :plain)))
      (.drawString g ch (* col w) (- (* row h) (quot @fontsize 4) 1))
      (swap! char-cache assoc k footprint))))

(defn- handle-keydown
  [fun e]
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
                  (= raw 13) "\r"
                  (= raw 27) "esc"
                  true (str (char raw)))]
    (when (and key (not= code 65406) (not= code 16)) (fun key))))

(defn redraw-frame
  []
  (let [tmpg (.getGraphics (BufferedImage. 40 40 BufferedImage/TYPE_INT_RGB))]
    (.setFont tmpg @font)
    (reset! fontwidth (.stringWidth (.getFontMetrics tmpg) "M"))
    (reset! fontheight (+ (.getHeight (.getFontMetrics tmpg)) 1))
    ;(editor/set-frame-dimensions (quot (.getHeight @panel) @fontheight) (quot (.getWidth @panel) @fontwidth))
    ;(reset! rows (editor/get-frame-rows))
    ; (reset! columns (editor/get-frame-columns))
    (view-draw)))

(defn set-font
  [font-name font-size]
  (reset! fontsize font-size)
  (update-font font-name)
  (redraw-frame)
  )


(defn jframequit
  []
  (System/exit 0))

(def ^:private updater (atom nil))
(def ^:private queue (atom []))

(defn print-buffer
  [g buf]
  (let [cache-id (buffer-footprint buf)
        w (buf ::buffer/window)
        top (w ::buffer/top)   ; Window top margin
        left (w ::buffer/left) ; Window left margin
        rows (w ::buffer/rows) ; Window rows
        cols (w ::buffer/cols) ; Window cols
        tow (buf ::buffer/tow) ; Top of window
        crow (-> buf ::buffer/cursor ::buffer/row)  ; Cursor row
        ccol (-> buf ::buffer/cursor ::buffer/col)] ; Cursor col
    (when-let [statusline (buf :status-line)]
      (print-buffer g statusline))

    ;; Looping over the rows and cols in buffer window in the terminal
    (loop [trow top  ; Terminal row
           tcol left ; Terminal col
           row (tow ::buffer/row)
           col (tow ::buffer/col)
           cursor-row nil
           cursor-col nil]
      (if (< trow (+ rows top))
        (do
        ;; Check if row has changed...
          ;(println "--C" "top" top "left" left "rows" rows "cols" cols "trow" trow "tcol" tcol "row" row color bgcolor)
          (let [cursor-match (or (and (= row crow) (= col ccol))
                                 (and (= row crow) (not cursor-col) (> col ccol))
                                 (and (not cursor-row) (> row crow)))
                cm (or (-> buf ::buffer/lines (get (dec row)) (get (dec col))) {}) ; Char map like {::buffer/char \x ::buffer/style :string} 
                c (or ; (when (and cursor-match (buf :status-line)) "█") 
                      (cm ::buffer/char)
                      ;\space
                      (if (and (= col 1) (> row (buffer/line-count buf))) "~" \space)
                      )
                new-cursor-row (if cursor-match trow cursor-row)
                new-cursor-col (if cursor-match tcol cursor-col)
                color (or (cm ::buffer/style) :plain)
                bgcolor (cond cursor-match :cursor1
                              (buffer/selected? buf row col) :selection
                              true :plain)
                last-col (+ cols left -1)
                n-trow (if (< last-col tcol) (inc trow) trow)
                n-tcol (if (< last-col tcol) left (inc tcol))
                n-row (cond (and (< last-col tcol) (> col (buffer/col-count buf row))) (buffer/next-visible-row buf row)
                            true row)
                n-col (cond (and (< last-col tcol) (> col (buffer/col-count buf row))) 1
                           true (inc col))]
              (draw-char g (str c) trow tcol color bgcolor)
              ;(when (and (= col (buffer/col-count buf row)) (> (buffer/next-visible-row buf row) (+ row 1))) (tty-print "…"))
              (recur n-trow n-tcol n-row n-col new-cursor-row new-cursor-col)))
        (when (buf :status-line)
          ;(tty-print esc cursor-row ";" cursor-col "H" esc "s" (or (buffer/get-char buf) \space))
          ;(tty-print esc "?25h" esc cursor-row ";" cursor-col "H" esc "s")
          (reset! last-buffer cache-id))))))

(defn- draw
  [g]
  (.setFont g @font)
;  (when (not @updater) (reset! updater (future nil)))
;  (when (future-done? @updater)
;    (reset! updater
;      (future

;        (draw-char g (str (rand-int 10)) 12 12 :green :plain)
        (while (not (empty? @queue))
          (when-let [b (first @queue)]
            (swap! queue #(subvec % 1))
            (print-buffer g b)))
;   )))
   )

(defn printer
  [buf]
  (let [fp (buffer-footprint buf)]
    ;; Replace outdated versions of buf 
    (swap! queue
      (fn [q] (conj
                (filterv #(not= (buffer-footprint %) fp) q)
                buf)))
    (future (view-draw))))

(defn init
  [fun & {:keys [font-name font-size]}]
  (when font-size (reset! fontsize font-size))
  (update-font)
  ;(update-font "Monospace Regular")
  (update-font "Liberation Mono")
  ;(if font-name (update-font font-name) (update-font))
  (let [icon (io/resource "liquid.png")
        tmpg (.getGraphics (BufferedImage. 40 40 BufferedImage/TYPE_INT_RGB))]
    (.setFont tmpg @font)
    (reset! fontwidth (.stringWidth (.getFontMetrics tmpg) "M"))
    (reset! fontheight (+ (.getHeight (.getFontMetrics tmpg)) 1))

    (reset! panel
      (proxy [JPanel] []
        (paintComponent [g]
          (draw g))))
    (.setFocusTraversalKeysEnabled @panel false)
    (.setPreferredSize @panel (Dimension. (* @cols @fontwidth) (* @rows @fontheight)))
    (.setDoubleBuffered @panel true)

    (reset! frame (JFrame. "λiquid"))
    (.setDefaultCloseOperation @frame (JFrame/EXIT_ON_CLOSE))
    (.setContentPane @frame @panel)
    (.setBackground @frame (@bgcolors :plain))
    (.setFocusTraversalKeysEnabled @frame false)
    (.addKeyListener @frame
      (proxy [KeyListener] []
        (keyPressed [e] (handle-keydown fun e))

        (keyReleased [e] (do))
        (keyTyped [e] (do))))
;    (.addWindowListener @frame
;      (proxy [WindowAdapter] []
;        (windowActivated [e] (editor/request-fullupdate) (view-draw))))
    (.setIconImage @frame (when icon (.getImage (ImageIcon. icon))))
    (.pack @frame)
    (.show @frame)
    ;(add-watch editor/updates "jframe" view-handler)
    ;(editor/request-fullupdate)
    ;(editor/updated)
    (view-draw)
    (Thread/sleep 400)
    ;(editor/request-fullupdate)
    ;(editor/updated)
    (view-draw)
    (.addComponentListener @frame
      (proxy [ComponentListener] []
        (componentShown [c] (redraw-frame))
        (componentMoved [c] (redraw-frame))
        (componentHidden [c] (redraw-frame))
        (componentResized [c] (redraw-frame))))
    ))

(defn exit-handler
  []
  (System/exit 0))

(def output-handler
  {:printer printer
   :dimensions (fn [] {:rows @rows :cols @cols})})