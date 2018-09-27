(ns dk.salza.liq.window
  (:require [dk.salza.liq.buffer :as buffer]
            [dk.salza.liq.tools.fileutil :as futil]
            [dk.salza.liq.slider :refer :all :rename {create create-slider}]))

;     123456789012345
;    1|abc--|abcdef--
;    2| w1  | w2
;    3|-----|--------
; 
; Screen 3x15
; w1: left 1 top 1 rows 2 columns 3
; w2: left 7 top 1 rows 2 columns 6
; left1 = left0 + columns0 + 3

(defn create
  [name top left rows columns buffername]
  {::name name
   ::top top
   ::left left
   ::rows rows
   ::columns columns
   ::buffername buffername})

(defn get-buffername
  [window]
  (window ::buffername))

(defn get-name
  [window]
  (window ::name))

(defn get-left
  [window]
  (window ::left))

(defn get-top
  [window]
  (window ::top))

(defn get-rows
  [window]
  (window ::rows))

(defn get-columns
  [window]
  (window ::columns))

(defn get-towid
  "Top-of-window id"
  [window buffer]
  (str (get-name window) "-" (get-buffername window)))

(defn split-window-right
  "Amount integer then absolute,
  if decimal, like 0.5, 0.1, 0.9 then
  relative."
  [windowlist amount]
  (let [w0 (first windowlist)
        wn (rest windowlist)
        absolute (if (integer? amount)
                     amount
                     (- (int (* (w0 ::columns) amount)) 1))]
    (conj wn
      (assoc w0
        ::name (str (w0 ::name) "-right-" (+ (rand-int 8999) 1000))
        ::left (+ (w0 ::left) absolute 3)
        ::columns (- (w0 ::columns) absolute 3))
      (assoc w0
        ::columns absolute))))

(defn split-window-below
  "Amount integer then absolute,
  if decimal, like 0.5, 0.1, 0.9 then
  relative."
  [windowlist amount]
  (let [w0 (first windowlist)
        wn (rest windowlist)
        absolute (if (integer? amount)
                     amount
                     (- (int (* (w0 ::rows) amount)) 1))]
    (conj wn
      (assoc w0
        ::name (str (w0 ::name) "-below" (+ (rand-int 8999) 1000))
        ::top (+ (w0 ::top) absolute 1)
        ::rows (- (w0 ::rows) absolute 1))
      (assoc w0
        ::rows absolute))))

(defn right-adjacent
  "Returns a list of windows where
  where the left border is adjacent to
  the right border of the first window.
  If now windows do that and empty list
  is returned."
  [windowlist]
  (let [w0 (first windowlist)
        top (w0 ::top)
        buttom (+ top (w0 ::rows))
        right (+ (w0 ::left) (w0 ::columns))
        wn (rest windowlist)]
    (filter
      (fn [w]
        (and
          (= (- (w ::left) right) 3) 
          (or
            (<= top (w ::top) buttom)
            (<= top (+ (w ::top) (w ::rows)) buttom))))
      wn)))

(defn right-aligned
  ""
  [windowlist]
  (let [w0 (first windowlist)
        ra (right-adjacent windowlist)]
    (cond (empty? ra) ra
          (and
            (= (apply min (map ::top ra)) (w0 ::top)) 
            (= (apply max (map #(+ (% ::top) (% ::rows)) ra)) (+ (w0 ::top) (w0 ::rows))))
          ra
          :else (list))))

(defn top-adjacent
  ""
  [windowlist]
  (let [w0 (first windowlist)
        top (w0 ::top)
        left (w0 ::left)
        right (+ left (w0 ::columns))
        wn (rest windowlist)]
    (filter
      (fn [w]
        (and
          (= (- top (w ::top) (w ::rows)) 1) 
          (or
            (<= left (w ::left) right)
            (<= left (+ (w ::left) (w ::columns)) right))))
      wn)))

(defn top-aligned
  ""
  [windowlist]
  (let [w0 (first windowlist)
        ta (top-adjacent windowlist)]
    (cond (empty? ta) ta
          (and
            (= (apply min (map ::left ta)) (w0 ::left)) 
            (= (apply max (map #(+ (% ::left) (% ::columns)) ta)) (+ (w0 ::left) (w0 ::columns))))
          ta
          :else (list))))

(defn left-adjacent
  ""
  [windowlist]
  (let [w0 (first windowlist)
        top (w0 ::top)
        buttom (+ top (w0 ::rows))
        left (w0 ::left)
        wn (rest windowlist)]
    (filter
      (fn [w]
        (and
          (= (- left (w ::left) (w ::columns)) 3) 
          (or
            (<= top (w ::top) buttom)
            (<= top (+ (w ::top) (w ::rows)) buttom))))
      wn)))

(defn left-aligned
  ""
  [windowlist]
  (let [w0 (first windowlist)
        la (left-adjacent windowlist)]
    (cond (empty? la) la
          (and
            (= (apply min (map ::top la)) (w0 ::top)) 
            (= (apply max (map #(+ (% ::top) (% ::rows)) la)) (+ (w0 ::top) (w0 ::rows))))
          la
          :else (list))))

(defn buttom-adjacent
  ""
  [windowlist]
  (let [w0 (first windowlist)
        buttom (+ (w0 ::top) (w0 ::rows))
        left (w0 ::left)
        right (+ left (w0 ::columns))
        wn (rest windowlist)]
    (filter
      (fn [w]
        (and
          (= (- (w ::top) buttom) 1) 
          (or
            (<= left (w ::left) right)
            (<= left (+ (w ::left) (w ::columns)) right))))
      wn)))

(defn buttom-aligned
  ""
  [windowlist]
  (let [w0 (first windowlist)
        ba (buttom-adjacent windowlist)]
    (cond (empty? ba) ba
          (and
            (= (apply min (map ::left ba)) (w0 ::left)) 
            (= (apply max (map #(+ (% ::left) (% ::columns)) ba)) (+ (w0 ::left) (w0 ::columns))))
          ba
          :else (list))))

(defn enlarge-window-right
  [windowlist amount]
  (let [w0 (first windowlist)
        columns (w0 ::columns)
        ra (right-aligned windowlist)
        maxamount (if (empty? ra) 0 (- (apply min (map ::columns ra)) 6))
        calcamount (min amount maxamount)
        cmpl (remove (set ra) (rest windowlist))]
    (if (<= calcamount 0)
      windowlist
      (concat
        (list (assoc w0 ::columns (+ columns calcamount)))
        (map #(assoc % ::left (+ (% ::left) calcamount)
                       ::columns (- (% ::columns) calcamount))
             ra)
        cmpl))))  

(defn shrink-window-right
  [windowlist amount]
  (let [w0 (first windowlist)
        columns (w0 ::columns)
        ra (right-aligned windowlist)
        maxamount (if (empty? ra) 0 (- (w0 ::columns) 6))
        calcamount (min amount maxamount)
        cmpl (remove (set ra) (rest windowlist))]
    (if (<= calcamount 0)
      windowlist
      (concat
        (list (assoc w0 ::columns (- columns calcamount)))
        (map #(assoc % ::left (- (% ::left) calcamount)
                       ::columns (+ (% ::columns) calcamount))
             ra)
        cmpl))))  

(defn enlarge-window-below
  [windowlist amount]
  (let [w0 (first windowlist)
        rows (w0 ::rows)
        ba (buttom-aligned windowlist)
        maxamount (if (empty? ba) 0 (- (apply min (map ::rows ba)) 3))
        calcamount (min amount maxamount)
        cmpl (remove (set ba) (rest windowlist))]
    (if (<= calcamount 0)
      windowlist
      (concat
        (list (assoc w0 ::rows (+ rows calcamount)))
        (map #(assoc % ::top (+ (% ::top) calcamount)
                       ::rows (- (% ::rows) calcamount))
             ba)
        cmpl))))  

(defn shrink-window-below
  [windowlist amount]
  (let [w0 (first windowlist)
        rows (w0 ::rows)
        ba (buttom-aligned windowlist)
        maxamount (if (empty? ba) 0 (- rows 3))
        calcamount (min amount maxamount)
        cmpl (remove (set ba) (rest windowlist))]
    (if (<= calcamount 0)
      windowlist
      (concat
        (list (assoc w0 ::rows (- rows calcamount)))
        (map #(assoc % ::top (- (% ::top) calcamount)
                       ::rows (+ (% ::rows) calcamount))
             ba)
        cmpl))))  


(defn delete-window
  "Takes a list of windows, and if possible
  removes the first window and adjusts the
  rest to fit the frame.
  
  Not implemented yet"
  [windowlist]
  (if (< (count windowlist) 2)
    windowlist
    (let [w0 (first windowlist)
          ra (right-aligned windowlist)
          rcmpl (remove (set ra) (rest windowlist))
          ta (top-aligned windowlist)
          tcmpl (remove (set ta) (rest windowlist))
          la (left-aligned windowlist)
          lcmpl (remove (set la) (rest windowlist))
          ba (buttom-aligned windowlist)
          bcmpl (remove (set ba) (rest windowlist))]
      (cond (not-empty ra)
            (concat (map #(assoc % ::left (w0 ::left)
                                   ::columns (+ (w0 ::columns) (% ::columns) 3))
                         ra) rcmpl)
            (not-empty ta)
            (concat (map #(assoc % ::rows (+ (% ::rows) (w0 ::rows) 1)) ta) tcmpl)
            (not-empty la)
            (concat (map #(assoc % ::columns (+ (w0 ::columns) (% ::columns) 3)) la) lcmpl)
            (not-empty ba)
            (concat (map #(assoc % ::top (w0 ::top)
                                   ::rows (+ (% ::rows) (w0 ::rows) 1))
                         ba) bcmpl)
            :else windowlist))))

;   ::top top
;   ::left left
;   ::rows rows
;   ::columns columns

; (doseq [x (remove (set (list 1 2)) (list 1 3 4))] (println x))


