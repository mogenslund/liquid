(ns dk.salza.liq.renderer
  (:require [dk.salza.liq.tools.fileutil :as fileutil]
            [dk.salza.liq.coreutil :refer :all]
            [dk.salza.liq.buffer :as buffer]
            [dk.salza.liq.window :as window]
            [dk.salza.liq.editor :as editor]
            [clojure.string :as str])
  (:use [dk.salza.liq.slider :as slider :exclude [create]]))

;(def top-of-window (atom {})) ; Keys are windowname-buffername

(defn- apply-br-and-update-tow
  "Returns a slider where cursor mark has been set,
  linebreaks on long lines created and point moved
  to top of window."
  [sl rows columns towid tow]
  (let [sl0 (-> sl (set-mark "cursor") (set-point tow))
        left-linebreaks (fn [s n]
                            (nth
                              (iterate #(-> % (left 1) (left-until #"\n")) s)
                              n))
        ;; add-br goes a visual line forward if the break is soft insert a hard.
        add-br (fn [s] (let [s0 (forward-line s columns)]
                         (cond (= (-> s0 (left 1) get-char) "\n") s0
                               (end? s0) s0
                               (= (get-mark s0 "cursor") (get-point s0)) (-> s0 (insert "\n") (set-mark "cursor"))
                               :else (insert s0 "\n"))))
        update-and-restore-point (fn [s newtow]
                                     (when (not= (@editor/top-of-window towid) newtow)
                                        (swap! editor/top-of-window assoc towid newtow))
                                     (set-point s newtow))]
    (if (< (get-mark sl0 "cursor") tow) ;; If point is before top of window
      (let [newtow (get-point (left-linebreaks sl0 (inc rows)))]
        (apply-br-and-update-tow sl rows columns towid newtow))
      (let [;; Add rows number of breaks
            sllist (iterate add-br sl0)
            slbefore (nth (iterate add-br sl0) (dec rows))
            sl1 (nth (iterate add-br sl0) rows)]
  
        ;; If original point is on the first rows of lines we are done
        ;; otherwise a recenter should be performed
        ;(futil/log (str (get-mark sl1 "cursor") ", " (get-point sl1) ", " (pr-str sl1)))
        (if ((if (and (end? sl1) (= (get-point slbefore) (get-point sl1))) <= <) (get-mark sl1 "cursor") (get-point sl1))
          (update-and-restore-point sl1 tow)
          (let [sl2 (loop [s sl1]
                      (if (<= (get-mark s "cursor") (get-point s))
                        s
                        (recur (add-br s))))
                ;; Now sl2 ends with the cursor
                sl3 (right (left-linebreaks sl2 (int (* rows 0.4))) 1)]
            ;; sl3 now has point at new top of window
            (update-and-restore-point sl3 (get-point sl3))
            (apply-br-and-update-tow sl rows columns towid (get-point sl3))
            ))))))

(defn insert-token
  [sl token]
  (assoc sl
    ::slider/before (conj (sl ::slider/before) token)
    ::slider/point (+ (sl ::slider/point) 1)
    ::slider/marks (slide-marks (sl ::slider/marks) (+ (sl ::slider/point) 0) 1)))

(defn apply-syntax-highlight
  [sl rows towid cursor-color syntaxhighlighter]
  (loop [sl0 sl n 0 face :plain bgface :plain pch "" ppch ""]
     (if (> n rows)
       (set-point sl0 (@editor/top-of-window towid))
       (let [ch (get-char sl0)
             p (get-point sl0)
             selection (get-mark sl0 "selection")
             cursor (get-mark sl0 "cursor")
             nextface (syntaxhighlighter sl0 face)
             nextbgface (cond (= p cursor) (if (= cursor-color :green) :cursor1 :cursor2)
                              (and selection (>= p (min selection cursor)) (< p (max selection cursor))) :selection
                              (and selection (>= p (max selection cursor))) :plain
                              (or (= bgface :cursor1) (= bgface :cursor2)) :plain
                              :else bgface)
             next (if (and (= nextface face)
                           (= nextbgface bgface)
                           (not (and (= pch "\n") (or (= nextface :string) (= nextbgface :selection)))))
                      sl0
                      (if (and (or (= nextbgface :cursor1) (= nextbgface :cursor2)) (or (= ch "\n") (end? sl0)))
                          (insert (insert-token sl0 {:face nextface :bgface nextbgface}) " ")
                          (insert-token sl0 {:face nextface :bgface nextbgface})))
                      ]
         (recur (right next 1)
                (if (or (= ch "\n") (= ch nil)) (inc n) n)
                nextface
                nextbgface
                ch
                pch
                )))))

(defn split-to-lines
  "Takes a list of chars and splits into
  a list of lists. Splitting where the char
  is a newline character."
  [charlist n]
  (map #(if (empty? (first %)) '("") (first %))
       (take n (iterate
                 (fn [x]
                   (split-with #(not= % "\n") (rest (second x))))
                 (split-with #(not= % "\n") charlist)))))

;;; ("a" "\n" "\n" "\n" "b" "c" "\n" "d") ---> (("a") ("") ("") ("b" "c") ("d"))

(defn render-window
  [window buffer]
  (let [;bmode (buffer/get-mode buffer)
        ;cursor-color (-> bmode ::mode/actionmapping first :cursor-color)
        cursor-color (buffer/get-action buffer :cursor-color)
        rows (window ::window/rows)
        columns (window ::window/columns)
        towid (str (window ::window/name) "-" (window ::window/buffername))
        tow (or (@editor/top-of-window towid) 0)
        sl (set-mark (buffer/get-slider buffer) "cursor")
        sl0 (apply-br-and-update-tow sl rows columns towid tow)
        ;tmp (futil/log (get-mark sl "cursor"))
        ;tmp1 (futil/log (get-mark sl0 "cursor"))
        filename (or (buffer/get-filename buffer) (buffer/get-name buffer) "")
        syntaxhighlighter  (or (-> buffer ::buffer/highlighter) (fn [sl face] :plain))
        sl1 (apply-syntax-highlight sl0 rows towid cursor-color  syntaxhighlighter)
        timestamp (.format (java.text.SimpleDateFormat. "yyyy-MM-dd HH:mm") (new java.util.Date))
        dirty (buffer/get-dirty buffer)
        statuslinecontent (str "L" (format "%-6s" (buffer/get-linenumber buffer))
                               timestamp
                               (if (and filename dirty) "  *  " "     ") filename)
        statusline (conj (map str (seq (subs (format (str "%-" (+ columns 1) "s") statuslinecontent)
                                             0 columns))) {:face :plain :bgface :statusline})
        lines (concat (split-to-lines (sl1 ::slider/after) rows) [statusline])]
      (map #(hash-map :row (+ %1 (window ::window/top))
                       :column (window ::window/left)
                       :line %2) (range (inc rows)) lines)
     ))

(defn render-screen
  []
  (let [windows (reverse (editor/get-windows))
        buffers (map #(editor/get-buffer (window/get-buffername %)) windows)]
     (doall (map render-window windows buffers))))