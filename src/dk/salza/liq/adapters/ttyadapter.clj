(ns dk.salza.liq.adapters.ttyadapter
  (:require [dk.salza.liq.apis :refer :all]
            [dk.salza.liq.util :as util]
            [dk.salza.liq.keys :as keys]
            [clojure.string :as str]))

(def esc (str "\033" "["))

(defn ttyrows
  []
  (let [shellinfo (with-out-str (util/cmd "/bin/sh" "-c" "stty size </dev/tty"))]
    (Integer/parseInt (re-find #"^\d+" shellinfo)))) ; (re-find #"\d+$" "50 120")

(defn ttycolumns
  []
  (let [shellinfo (with-out-str (util/cmd "/bin/sh" "-c" "stty size </dev/tty"))]
    (Integer/parseInt (re-find #"\d+$" shellinfo)))) ; (re-find #"\d+$" "50 120")

(defn ttywait-for-input
  []
  ;(util/cmd "/bin/sh" "-c" "stty -echo raw </dev/tty")
  (let [r (java.io.BufferedReader. *in*)
        input (+ (.read r) (if (.ready r) (* 256 (+ (.read r) 1)) 0) (if (.ready r) (* 256 256 (+ (.read r) 1)) 0))]
    ;(spit "/tmp/keys.txt" (str (pr-str input) " - " "" (keys/raw2keyword input) "\n") :append true)
    (keys/raw2keyword input)))

(def old-lines (atom {}))
;Black       0;30     Dark Gray     1;30
;Blue        0;34     Light Blue    1;34
;Green       0;32     Light Green   1;32
;Cyan        0;36     Light Cyan    1;36
;Red         0;31     Light Red     1;31
;Purple      0;35     Light Purple  1;35
;Brown       0;33     Yellow        1;33
;Light Gray  0;37     White         1;37
;; http://misc.flogisoft.com/bash/tip_colors_and_formatting
;; http://ascii-table.com/ansi-escape-sequences.php

(defn print-color
  [index & strings] ;   0         1          2        3          4         5        6    7   8        9     10
  (let [colorpalette ["0;37" "38;5;131" "38;5;105" "38;5;11" "38;5;40" "38;5;117" "42" "44" "45" "48;5;235" "49"]]
    (print (str "\033[" (colorpalette index) "m" (apply str strings)))))

(defn ttyprint-lines
  [lines]
  ;; Redraw whole screen once in a while
  ;; (when (= (rand-int 100) 0)
  ;;  (reset! old-lines {})
  ;;  (print "\033[0;37m\033[2J"))
  (doseq [line lines]
    (let [row (line :row)
          column (line :column)
          content (line :line)
          key (str "k" row "-" column)
          oldcontent (@old-lines key)] 
    (when (not= oldcontent content)
      (let [diff (max 1 (- (count (filter #(and (string? %) (not= % "")) oldcontent))
                           (count (filter #(and (string? %) (not= % "")) content))))
            padding (format (str "%" diff "s") " ")]
        (print (str "\033[" row ";" column "H\033[s"))
        (print-color  9 " ")
        (print-color 0)
        (print-color 10)
        (doseq [ch (line :line)]
          (if (string? ch)
            (if (= ch "\t") (print (char 172)) (print ch)) 
            (do
              (cond (= (ch :face) :string) (print-color 1)
                    (= (ch :face) :comment) (print-color 2)
                    (= (ch :face) :type1) (print-color 3) ; defn
                    (= (ch :face) :type2) (print-color 4) ; function
                    (= (ch :face) :type3) (print-color 5) ; keyword
                    :else (print-color 0))
              (cond (= (ch :bgface) :cursor1) (print-color 6)
                    (= (ch :bgface) :cursor2) (print-color 7)
                    (= (ch :bgface) :selection) (print-color 8)
                    (= (ch :bgface) :statusline) (print-color 9)
                    :else (print-color 10))
            )))
        (if (= row (count lines))
          (do
            (print (str "  " padding))
            (print-color 0))
          (print-color 10 padding)))
      (swap! old-lines assoc key content))
    ))
  (flush))

(defn ttyreset
  []
  (reset! old-lines {})
  (print "\033[0;37m\033[2J"))
  

(defn ttyinit
  []
  (util/cmd "/bin/sh" "-c" "stty -echo raw </dev/tty")
  (print "\033[0;37m\033[2J")
  (print "\033[?25l") ; Hide cursor
  (print "\033[?7l") ; disable line wrap
  )

(defn ttyquit
  []
  (print "\033[0;37m\033[2J")
  (print "\033[?25h")
  (flush)
  (util/cmd "/bin/sh" "-c" "stty -echo cooked </dev/tty")
  (util/cmd "/bin/sh" "-c" "stty -echo sane </dev/tty")
  (println "")
  (System/exit 0))

(defrecord TtyAdapter []
  Adapter
  (init [this] (ttyinit))
  (rows [this] (ttyrows))
  (columns [this] (ttycolumns))
  (wait-for-input [this] (ttywait-for-input))
  (print-lines [this lines] (ttyprint-lines lines))
  (reset [this] (ttyreset))
  (quit [this] (ttyquit)))