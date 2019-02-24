(ns dk.salza.liq.adapters.tty
  (:require [dk.salza.liq.tools.util :as util]
            [dk.salza.liq.renderer :as renderer]
            [dk.salza.liq.editor :as editor]
            [dk.salza.liq.logging :as logging]
            [clojure.string :as str]))

(def ^:private old-lines (atom {}))
(def ^:private updater (atom (future nil)))
(def ^:private sysout (System/out))

(def esc "\033[")

(defn- tty-print
  [& args]
  (.print sysout (str/join "" args)))

(defn- tty-println
  [& args]
  (.println sysout (str/join "" args)))

(defn- reset
  []
  (tty-print esc "?25l") ; Hide cursor
  (tty-print esc "?7l")  ; disable line wrap
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

(def ^:private colors
  {:plain "0;40"
   :type1 "38;5;11"
   :type2 "38;5;40"
   :type3 "38;5;117"
   :green "38;5;40"
   :yellow "38;5;11"
   :red "38;5;196"
   :comment "38;5;105"
   :string "38;5;131"
   :stringst "38;5;131"})

(def ^:private bgcolors
  {:plain "49"
   :cursor0 "49"
   :cursor1 "42"
   :cursor2 "44"
   :hl "48;5;52"
   :selection "48;5;17"
   :statusline "48;5;235"})

(defn- print-color
  [color & strings]
  (tty-print esc color "m" (str/join strings)))

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
        (tty-print esc row ";" column "H" esc "s")
        (print-color (bgcolors :statusline) " ")
        (print-color (colors :plain))
        (print-color (bgcolors :plain))
        (doseq [ch (line :line)]
          (let [c (if (map? ch) (or (ch :char) "…") ch)
                face (when (map? ch) (ch :face))
                bgface (when (map? ch) (ch :bgface))]
            (when face (print-color (or (colors face) (colors :plain))))
            (when bgface (print-color (or (bgcolors bgface) (bgcolors :plain))))
            (cond (= c "\t") (tty-print (char 172))
                  (= c "\r") (tty-print (char 633))
                  true (tty-print c))))
        (if (= row (count (first lineslist)))
          (do
            (tty-print esc "K")
            (print-color (bgcolors :plain)))
          (print-color (bgcolors :plain)  padding)))
      (swap! old-lines assoc key content))
    ))
  (flush))

(defn- view-draw
  []
  (when (empty? @old-lines) (tty-print esc "0;37m" esc "2J"))
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
            (= raw2 '(27 91 49 59 53 72)) "C-home"
            (= raw2 '(27 91 49 59 53 70)) "C-end"
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
  (tty-print esc "0;37m" esc "2J")
  (tty-print esc "?25l") ; Hide cursor
  (tty-print esc "?7l")  ; disable line wrap
  (add-watch editor/updates "tty" view-handler))
