(ns liq.browser-io
  (:require [clojure.string :as str]
            [liq.buffer :as buffer]
            [liq.util :as util]))

(def ^:private rows (atom 40))
(def ^:private cols (atom 120))

(def ^:private last-buffer (atom nil))


(enable-console-print!)

(def mycounter (atom 0))

(defn resolve-key
  [e]
  (let [k (.-key e)]
    (cond (= k "Shift") nil 
          (= k "Control") nil 
          (= k "Alt") nil 
          (= k "AltGraph") nil 
          (= k "Escape") "esc" 
          (= k "Enter") "\n" 
          (> (count k) 1) (str/lower-case k)
          true k)))

(def char-cache (atom {}))
(defn- draw-char
  [ch row col color bgcolor]
  (let [k (str "r" row "c" col)
        footprint (str ch row col color bgcolor)]
    ;(.log js/console k)
    (when (and (not= (@char-cache k) footprint) (<= row @rows) (<= col @cols))
      (let [elem (.getElementById js/document k)]
        (set! (.-innerHTML elem) ch)
        (.log js/console (str (name color) " " (name bgcolor)))
        (.setAttribute elem "class" (str (name color) " " (name bgcolor))))
  ;    (-> js/document
  ;        (.getElementById k)
  ;        (.-innerHTML)
  ;        (set! ch))
      (swap! char-cache assoc k footprint))))


(defn keydown [fun e]
  ;(when (h/in? [32 37 38 39 40] (.-keyCode e)) (.preventDefault e))
  (.log js/console e)
  (let [k (resolve-key e)]
    (when k (fun k))
;  (-> js/document
;      (.getElementById "app")
;      (.-innerHTML)
;      (set! (str "<h1>It works</h1>" (.-key e) " " (.-keyCode e) " " @mycounter)))
  ))

(defn buffer-footprint
  [buf]
  [(buf ::buffer/window) (buf ::buffer/name) (buf ::buffer/file-name)])

(defn print-buffer
  [buf]
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
      (print-buffer statusline))

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
                n-trow (if (< cols tcol) (inc trow) trow)
                n-tcol (if (< cols tcol) left (inc tcol))
                n-row (cond (and (< cols tcol) (> col (buffer/col-count buf row))) (buffer/next-visible-row buf row)
                            true row)
                n-col (cond (and (< cols tcol) (> col (buffer/col-count buf row))) 1
                            true (inc col))]
              (draw-char (str c) trow tcol color bgcolor)
              ;(when (and (= col (buffer/col-count buf row)) (> (buffer/next-visible-row buf row) (+ row 1))) (tty-print "…"))
              (recur n-trow n-tcol n-row n-col new-cursor-row new-cursor-col)))
        (when (buf :status-line)
          ;(tty-print esc cursor-row ";" cursor-col "H" esc "s" (or (buffer/get-char buf) \space))
          ;(tty-print esc "?25h" esc cursor-row ";" cursor-col "H" esc "s")
          (reset! last-buffer cache-id))))))


(defn generate-text-area
  [rows cols]
  (str/join ""
    (for [r (range 1 (inc rows)) c (range 1 (inc cols))]
      (str "<span id=\"r" r "c" c "\" class=\"bgplain plain\"> </span>"
           (when (= c cols) "<br>\n")))))

(defn init
  [fun]
  (-> js/document
      (.getElementById "app")
      (.-innerHTML)
      (set! (generate-text-area @rows @cols)))
  (set! (.-onkeydown js/document) #(keydown fun %)))

(def ^:private queue (atom []))

(def ^:private next-buffer (atom nil))
(js/setInterval
   #(when-let [buf @next-buffer]
      (reset! next-buffer nil)
      (print-buffer buf))
      20)

(defn printer
  [buf]
  (reset! next-buffer buf))

(def output-handler
  {:printer printer
   :dimensions (fn [] {:rows @rows :cols @cols})})