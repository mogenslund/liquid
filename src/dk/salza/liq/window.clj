(ns dk.salza.liq.window
  (:require [dk.salza.liq.buffer :as buffer]
            [dk.salza.liq.tools.fileutil :as futil])
  (:use [dk.salza.liq.slider :as slider :exclude [create]]))

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

(defn get-towid
  "Top-of-window id"
  [window buffer]
  (str (get-name window) "-" (get-buffername window)))

(defn resize-width
  [window delta]
  (if (< (+ (window ::columns) delta) 3)
    window
    (update window ::columns #(+ % delta))))

(defn resize-height
  [window delta]
  (if (< (+ (window ::rows) delta) 3)
    window
    (update window ::rows #(+ % delta))))

(defn split-window-right
  "Amount integer then absolute,
  if decimal, like 0.5, 0.1, 0.9 then
  relative."
  [windowlist amount]
  (let [w0 (first windowlist)
        wn (rest windowlist)
        absolute (if (int? amount)
                     amount
                     (- (int (* (w0 ::columns) amount)) 1))]
    (conj wn
      (assoc w0
        ::name (str (w0 ::name) "-right")
        ::left (+ (w0 ::left) absolute 3)
        ::columns (- (w0 ::columns) absolute 2))
      (assoc w0
        ::columns absolute))))

(defn split-window-below
  "Amount integer then absolute,
  if decimal, like 0.5, 0.1, 0.9 then
  relative."
  [windowlist amount]
  (let [w0 (first windowlist)
        wn (rest windowlist)
        absolute (if (int? amount)
                     amount
                     (- (int (* (w0 ::rows) amount)) 1))]
    (conj wn
      (assoc w0
        ::name (str (w0 ::name) "-below")
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
        right (+ (w0 ::left) (w0 ::colunms))
        wn (rest windowlist)]
    (filter
      (fn [w]
        (and
          (< (- right (w ::left)) 5) 
          (or
            (and (<= top (w ::top))
                 (>= buttom (w ::top)))
            (and (<= top (+ (w ::top) (w ::rows)))
                 (>= buttom (+ (w ::top) (w ::rows)))))))
      wn)))

(defn right-aligned
  "Not implemented yet"
  [windowlist]
  (let [w0 (first windowlist)
        ra (right-adjacent windowlist)]
    (cond (empty? ra) ra
          (and
            (= (apply min (map ::top ra)) (w0 ::top)) 
            (= (apply max (map #(+ (% ::top) (% ::rows) ra))) (w0 ::top)))
          ra
          :else (list))))

(defn delete-window
  "Takes a list of windows, and if possible
  removes the first window and adjusts the
  rest to fit the frame.
  
  Not implemented yet"
  [windowlist]
  (if (< (count windowlist) 2)
    windowlist
    (let [ra (right-aligned windowlist)]
      (cond true (rest windowlist)
            (not-empty ra)
            (let [w0 (first windowlist)
                  cmpl (remove (set ra) (rest windowlist))]
              (concat (map #(assoc % ::left (w0 ::left))) cmpl))
            :else windowlist))))

;   ::top top
;   ::left left
;   ::rows rows
;   ::columns columns

; (doseq [x (remove (set (list 1 2)) (list 1 3 4))] (println x))