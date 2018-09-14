(ns dk.salza.liq.extensions.folding
  (:require [dk.salza.liq.slider :refer :all]
            [clojure.string :as str]))

(defn get-headline-level
  "Returns the headline level of the current line.
  Starting with # is level 1, ## is level 2 etc.
  If there is no headline, e.g no # then result is 0."
  [sl]
  (loop [n 0 s (-> sl beginning-of-line)]
    (if (not= (get-char s) "#")
      n
      (recur (inc n) (right s)))))

(defn line-folded?
  "Returns if the line is folded, somewhere.
  If there is hidden content somewhere on the line."
  [sl]
  (loop [s (beginning-of-line sl)]
    (cond (hidden? s) true
          (is-newline? (get-char s)) false
          (nil? (get-char s)) false
          true (recur (right s)))))

(defn sublevel-folded?
  [sl]
  (let [level (get-headline-level sl)]
    (loop [s (-> sl end-of-line right)]
      (cond (end? s) false
            (<= 1 (get-headline-level s) level) false
            (line-folded? s) true
            true (recur (-> s end-of-line right))))))

(defn unfold-line
  [sl]
  (let [p (get-point sl)
        sl1 (loop [s (beginning-of-line sl)]
              (cond (hidden? s) (unhide s) 
                    (is-newline? (get-char s)) s
              (nil? (get-char s)) s
              true (recur (right s))))]
    (set-point sl1 p)))

(defn fold-level
  [sl]
  (if (line-folded? sl)
    sl
    (let [p (get-point sl)
          level (get-headline-level sl)
          sl0 (-> sl
                  end-of-line
                 (set-mark "fold")
                 (right))]
      (if (= level 0)
        sl
        (loop [s sl0]
          (cond (end? s)
                (-> s
                    (hide-region "fold")
                    (set-point p))
                (<= 1 (get-headline-level s) level)
                (-> s
                    left
                    (hide-region "fold")
                    (set-point p))
                true
                (recur (-> s
                           end-of-line
                           right))))))))

(defn fold-def
  [sl]
  (let [p (get-point sl)]
    (-> sl
        (mark-paren-end "pend")
        (point-to-mark "pend")
        left
        (set-mark "fold")
        (set-point p)
        end-of-line
        (hide-region "fold")
        (set-point p))))

(defn fold-all-def
  [sl]
  (loop [s (beginning sl)]
    (if (end? s)
      (beginning s) 
      (recur
        (-> (if (= (get-char s) "(") (fold-def s) s) ;)
            end-of-line
            right)))))

(defn unfold-all
  [sl]
  (loop [s (beginning sl)]
    (if (end? s)
      (beginning s)
      (recur (-> s unhide right)))))

(defn unfold-all-level
  [sl level]
  (loop [s (-> sl unfold-all beginning)]
    (if (end? s)
      (beginning s)
      (recur
        (-> (if (>= (get-headline-level s) level) (fold-level s) s)
            end-of-line
            right)))))

(defn cycle-level-fold
  [sl]
  ;; In progress
  ;; Use recursion ... get-char which is slider, then unfold-all, etc
  (let [p (get-point sl)
        level (get-headline-level sl)]
    (cond (line-folded? sl)
          (loop [s (-> sl unfold-line end-of-line right)]
            (if (or (<= 1 (get-headline-level s) level) (end? s))
              (set-point s p)
              (recur
                (-> s
                    fold-level
                    end-of-line
                    right))))
          (= (-> sl beginning-of-line get-char) "(") (fold-def sl) ;)
          (sublevel-folded? sl)
          (loop [s (-> sl end-of-line right)]
            (if (or (end? s) (<= 1 (get-headline-level s) level))
              (set-point s p)
              (recur (-> s unfold-line end-of-line right))))
          true
          (fold-level sl))))

(defn context-collapse-toggle
  [sl]
  (let [p (get-point sl)]
    (if (line-folded? sl)
      (-> sl unfold-line (set-point p))
      (let [level (get-headline-level sl)]
        (if (= level 0)
          sl
          (loop [s (-> sl end-of-line (set-mark "collapse") right)]
            (cond (end? s) (-> s (hide-region "collapse") (set-point p))
                  (<= 1 (get-headline-level s) level) (-> s left (hide-region "collapse") (set-point p))
                  true (recur (-> s end-of-line right)))))))))

(defn collapse-all-level
  [sl level]
  (loop [s (beginning sl)]
    (if (end? s)
      s
      (recur (if (= (get-headline-level s) level)
               (-> s context-collapse-toggle end-of-line right)
               (-> s end-of-line right))))))

(defn expand-all
  [sl]
  (loop [s (beginning sl)]
    (if (end? s)
      (beginning s)
      (recur (-> s unhide right)))))

(defn collapse-all
  [sl]
  (-> sl
      expand-all
      fold-all-def
      (collapse-all-level 6)
      (collapse-all-level 5)
      (collapse-all-level 4)
      (collapse-all-level 3)
      (collapse-all-level 2)
      (collapse-all-level 1)
      beginning))
