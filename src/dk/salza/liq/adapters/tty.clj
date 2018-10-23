(ns dk.salza.liq.adapters.tty
  (:require [dk.salza.liq.tools.util :as util]
            [dk.salza.liq.renderer :as renderer]
            [dk.salza.liq.editor :as editor]
            [dk.salza.liq.logging :as logging]
            [clojure.string :as str]))

(def ^:private old-lines (atom {}))
(def ^:private updater (atom (future nil)))
(def ^:private sysout (System/out))

(defn- tty-print
  [arg]
  (.print sysout arg))

(defn- tty-println
  [arg]
  (.println sysout arg))

(defn- reset
  []
  (reset! old-lines {}))

(defn rows
  []
  (loop [shellinfo (with-out-str (util/cmd "/bin/sh" "-c" "stty size </dev/tty")) n 0]
    (if (or (re-find #"^\d+" shellinfo) (> n 10)) 
      (Integer/parseInt (re-find #"^\d+" shellinfo))
      (do
        (tty-println n)
        (Thread/sleep 100)
        (recur (with-out-str (util/cmd "/bin/sh" "-c" "stty size </dev/tty")) (inc n))))))

(defn columns
  []
  (loop [shellinfo (with-out-str (util/cmd "/bin/sh" "-c" "stty size </dev/tty")) n 0]
    (if (or (re-find #"\d+$" shellinfo) (> n 10)) 
      (Integer/parseInt (re-find #"\d+$" shellinfo))
      (do
        (tty-println n)
        (Thread/sleep 100)
        (recur (with-out-str (util/cmd "/bin/sh" "-c" "stty size </dev/tty")) (inc n))))))

;;                 0         1          2        3          4         5        6    7      8           9     10      11      12 green  13 yellow 14 red
(def ^:private colorpalette ["0;40" "38;5;131" "38;5;105" "38;5;11" "38;5;40" "38;5;117" "42" "44" "48;5;17" "48;5;235" "49" "48;5;52" "38;5;40" "38;5;11" "38;5;196"])

(defn- print-color
  [index & strings]
  (tty-print (str "\033[" (colorpalette index) "m" (str/join strings))))

(defn- print-lines
  [lineslist]
  (doseq [line (apply concat lineslist)]
    (let [row (line :row)
          column (line :column)
          content (line :line)
          key (str "k" row "-" column)
          oldcontent (@old-lines key)] 
    (when (not= oldcontent content)
      (let [diff (max 1 (- (count oldcontent)
                           (count content) -1))
            padding (format (str "%" diff "s") " ")]
        (tty-print (str "\033[" row ";" column "H\033[s"))
        (print-color  9 " ")
        (print-color 0)
        (print-color 10)
        (doseq [ch (line :line)]
          (let [c (if (map? ch) (or (ch :char) "…") ch)
                face (when (map? ch) (ch :face))
                bgface (when (map? ch) (ch :bgface))]
            (when face
              (cond (= face :string) (print-color 1)
                    (= face :stringst) (print-color 1)
                    (= face :comment) (print-color 2)
                    (= face :green) (print-color 12)  
                    (= face :yellow) (print-color 13)  
                    (= face :red) (print-color 14)  
                    (= face :type1) (print-color 3) ; defn
                    (= face :type2) (print-color 4) ; function
                    (= face :type3) (print-color 5) ; keyword
                    :else (print-color 0)))
            (when bgface
              (cond (= bgface :cursor0) (print-color 10)
                    (= bgface :cursor1) (print-color 6)
                    (= bgface :cursor2) (print-color 7)
                    (= bgface :hl) (print-color 11)
                    (= bgface :selection) (print-color 8)
                    (= bgface :statusline) (print-color 9)
                    :else (print-color 10)))
            (if (= c "\t") (tty-print (char 172)) (tty-print c))))
        (if (= row (count (first lineslist)))
          (do
            (tty-print (str "  " padding))
            (print-color 0))
          (print-color 10 padding)))
      (swap! old-lines assoc key content))
    ))
  (flush))


(defn- view-draw
  []
  (when (empty? @old-lines) (tty-print "\033[0;37m\033[2J"))
  (print-lines (renderer/render-screen)))

(defn- view-handler
  [key reference old new]
  (remove-watch editor/editor key)
  (when (editor/fullupdate?) (reset))
  (when (future-done? @updater)
    (reset! updater
      (future
        (loop [u @editor/updates]
          (view-draw)
          (when (not= u @editor/updates)
          (recur @editor/updates))))))
  (add-watch editor/updates key view-handler))

(defn- model-update
  [input]
  (editor/handle-input input))

;; http://ascii-table.com/ansi-escape-sequences.php
(defn- raw2keyword
  [raw]
  (logging/log "RAW\n" (pr-str raw)) 
  (if (integer? raw)
    (cond (= raw 127) "backspace"
          (>= raw 32) (str (char raw))
          (= raw 9) "\t"
          (= raw 13) "\n"
          (<= 1 raw 26) (str "C-" (char (+ raw 96)))
          (= raw 0) "C- "
          true (str (char raw)))
    (let [raw2 (conj (take-while #(not= % 27) (rest raw)) 27)
          c0 (first raw)
          c1 (second raw)
          n (count raw2)]
      (cond (and (= n 1) (= c0 27)) "esc"
            (and (= n 2) (>= c1 32)) (str "M-" (char c1))
            (and (= n 2) (= c1 13)) "M-\n"
            (and (= n 2) (= c0 27) (<= 1 c1 26)) (str "C-M-" (char (+ c1 96)))
            (and (= n 2) (= c0 27)) (str "M-" (char c1))
            (= raw2 '(27 91 65)) "up"
            (= raw2 '(27 91 66)) "down"
            (= raw2 '(27 91 67)) "right"
            (= raw2 '(27 91 68)) "left"
            (= raw2 '(27 91 72)) "home"
            (= raw2 '(27 91 70)) "end"
            (= raw2 '(27 91 53 126)) "pgup"
            (= raw2 '(27 91 54 126)) "pgdn"
            (= raw2 '(27 91 50 126)) "ins"
            (= raw2 '(27 91 51 126)) "del"
            (= raw2 '(27 79 81)) "f2"
            (= raw2 '(27 79 82)) "f3"
            (= raw2 '(27 79 83)) "f4"
            (= raw2 '(27 91 49 53 126)) "f5"
            (= raw2 '(27 91 49 55 126)) "f6"
            (= raw2 '(27 91 49 56 126)) "f7"
            (= raw2 '(27 91 49 57 126)) "f8"
            (= raw2 '(27 91 50 48 126)) "f9"
            (= raw2 '(27 91 50 52 126)) "f12"
            (= raw2 '(27 91 49 59 50 65)) "S-up"
            (= raw2 '(27 91 49 59 50 66)) "S-down"
            (= raw2 '(27 91 49 59 50 68)) "S-left"
            (= raw2 '(27 91 49 59 50 67)) "S-right"
            true (str (char c0))))))

(defn input-handler
  []
  (future
    (let [r (java.io.BufferedReader. *in*)
          read-input (fn [] (raw2keyword
                              (let [input0 (.read r)]
                                (if (= input0 27)
                                  (loop [res (list input0)]
                                    (Thread/sleep 1)
                                    (if (not (.ready r))
                                      (reverse res)
                                      (recur (conj res (.read r)))))
                                  input0))))]
      (loop [input (read-input)]
        (logging/log "INPUT" input) 
        (model-update input)
        (recur (read-input))))))

(defn view-init
  []
  (util/cmd "/bin/sh" "-c" "stty -echo raw </dev/tty")
  (tty-print "\033[0;37m\033[2J")
  (tty-print "\033[?25l") ; Hide cursor
  (tty-print "\033[?7l") ; disable line wrap
  (add-watch editor/updates "tty" view-handler))
