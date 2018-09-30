(ns dk.salza.liq.renderer
  (:require [dk.salza.liq.tools.fileutil :as fileutil]
            [dk.salza.liq.coreutil :refer :all]
            [dk.salza.liq.slider :as slider]
            [dk.salza.liq.buffer :as buffer]
            [dk.salza.liq.window :as window]
            [dk.salza.liq.editor :as editor]
            [clojure.string :as str]
            [dk.salza.liq.slider :refer :all]))

(defn- apply-syntax-highlight
  [sl rows towid cursor-color syntaxhighlighter active]
  (loop [sl0 sl n 0 face :plain bgface :plain pch "" ppch ""]
     (if (> n rows)
       (set-point sl0 (editor/get-top-of-window towid))
       (let [ch (get-char sl0)
             p (get-point sl0)
             selection (get-mark sl0 "selection")
             cursor (get-mark sl0 "cursor")
             paren-start (get-mark sl0 "hl0")
             paren-end (get-mark sl0 "hl1")
             nextface (syntaxhighlighter sl0 face)
             nextbgface (cond (and (= p cursor) (not= cursor-color :off))
                                (cond (not active) :cursor0
                                      (= cursor-color :green) :cursor1
                                      :else :cursor2)
                              (= p paren-start) :hl
                              (= (+ p 1) paren-end) :hl
                              (and selection (>= p (min selection cursor)) (< p (max selection cursor))) :selection
                              (and selection (>= p (max selection cursor))) :plain
                              (or (= bgface :cursor0) (= bgface :cursor1) (= bgface :cursor2) (= bgface :hl)) :plain
                              :else bgface)
             next (if (and (= nextface face)
                           (= nextbgface bgface)
                           (not (and (= pch "\n") (or (= nextface :string) (= nextbgface :selection)))))
                      (right sl0)
                      (if (and (or (= nextbgface :cursor0) (= nextbgface :cursor1) (= nextbgface :cursor2)) (or (= ch "\n") (end? sl0)))
                          (-> sl0 (insert " ") left (set-meta :face nextface) (set-meta :bgface nextbgface) right right)
                          (-> sl0 (set-meta :face nextface) (set-meta :bgface nextbgface) right)))]
         (recur next
                (if (or (= ch "\n") (= ch nil)) (inc n) n)
                nextface
                nextbgface
                ch
                pch
                )))))

(defn- newline?
  [c]
  (if (string? c)
    (= c "\n")
    (= (c :char) "\n")))
  
(defn split-to-lines
  "Takes a list of chars and splits into
  a list of lists. Splitting where the char
  is a newline character."
  [charlist n]
  (map #(if (empty? (first %)) (list "") (first %))
       (take n 
               (iterate
                 (fn [x]
                   (split-with #(not (newline? %)) (rest (second x))))
                 (split-with #(not (newline? %)) charlist))
         )))

;;; ("a" "\n" "\n" "\n" "b" "c" "\n" "d") ---> (("a") ("") ("") ("b" "c") ("d"))

(defn render-window
  [window buffer]
  (let [cursor-color (buffer/get-action buffer :cursor-color)
        rows (window/get-rows window)
        columns (window/get-columns window)
        towid (window/get-towid window buffer)
        tow (or (editor/get-top-of-window towid) 0)
        active (= window (editor/current-window))
        sl (set-mark (buffer/get-slider buffer) "cursor")

        sl0 (update-top-of-window sl rows columns tow)
        tmp-tmp-tmp (editor/set-top-of-window towid (get-point sl0))

        filename (or (buffer/get-filename buffer) (buffer/get-name buffer) "")
        syntaxhighlighter  (or (buffer/get-highlighter buffer) (fn [sl face] :plain))
        sl1 (apply-syntax-highlight sl0 rows towid cursor-color syntaxhighlighter active)
        timestamp (.format (java.text.SimpleDateFormat. "yyyy-MM-dd HH:mm") (new java.util.Date))
        dirty (buffer/dirty? buffer)
        statuslinecontent (str (format "%-6s" (-> (buffer/get-slider buffer) slider/get-linenumber))
                               timestamp
                               (if (and filename dirty) "  *  " "     ") filename)
        statusline (conj (map str (seq (subs (format (str "%-" (+ columns 3) "s") statuslinecontent)
                                             0 (+ columns 2)))) {:char "L" :face :plain :bgface :statusline})
        lines (concat (split-to-lines (get-after-list sl1) rows) [statusline])]
      (map #(hash-map :row (+ %1 (window/get-top window))
                      :column (window/get-left window)
                      :line %2) (range (inc rows)) lines)))

(defn render-screen
  []
  (let [windows (reverse (editor/get-windows))
        buffers (map #(editor/get-buffer (window/get-buffername %)) windows)]
     (doall (map render-window windows buffers))))