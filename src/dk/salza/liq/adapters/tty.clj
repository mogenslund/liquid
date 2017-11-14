(ns dk.salza.liq.adapters.tty
  (:require [dk.salza.liq.tools.util :as util]
            [dk.salza.liq.keys :as keys]
            [dk.salza.liq.renderer :as renderer]
            [dk.salza.liq.editor :as editor]
            [dk.salza.liq.logging :as logging]
            [clojure.string :as str]))

(def old-lines (atom {}))
(def updater (ref (future nil)))
(def sysout (System/out))

(defn tty-print
  [arg]
  ;(print arg))
  (.print sysout arg))

(defn tty-println
  [arg]
  ;(println arg))
  (.println sysout arg))

(defn reset
  []
  (reset! old-lines {})
  ;(tty-print "\033[0;37m\033[2J")
  )

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

(defn print-color
  [index & strings] ;   0         1          2        3          4         5        6    7   8        9     10    11  12 green  13 yellow 14 red
  (let [colorpalette ["0;40" "38;5;131" "38;5;105" "38;5;11" "38;5;40" "38;5;117" "42" "44" "45" "48;5;235" "49" "43" "38;5;40" "38;5;11" "38;5;196"]]
    (tty-print (str "\033[" (colorpalette index) "m" (apply str strings)))))

(defn print-lines
  [lineslist]
  ;(reset)
  ;; Redraw whole screen once in a while
  ;; (when (= (rand-int 100) 0)
  ;;  (reset! old-lines {})
  ;;  (tty-print "\033[0;37m\033[2J"))
  (doseq [line (apply concat lineslist)]
    (let [row (line :row)
          column (line :column)
          content (line :line)
          key (str "k" row "-" column)
          oldcontent (@old-lines key)] 
    (when (not= oldcontent content)
      (let [diff (max 1 (- (count (filter #(and (string? %) (not= % "")) oldcontent))
                           (count (filter #(and (string? %) (not= % "")) content))))
            padding (format (str "%" diff "s") " ")]
        (tty-print (str "\033[" row ";" column "H\033[s"))
        (print-color  9 " ")
        (print-color 0)
        (print-color 10)
        (doseq [ch (line :line)]
          (if (string? ch)
            (if (= ch "\t") (tty-print (char 172)) (tty-print ch)) 
            (do
              (cond (= (ch :face) :string) (print-color 1)
                    (= (ch :face) :comment) (print-color 2)
                    (= (ch :face) :green) (print-color 12)  
                    (= (ch :face) :yellow) (print-color 13)  
                    (= (ch :face) :red) (print-color 14)  
                    (= (ch :face) :type1) (print-color 3) ; defn
                    (= (ch :face) :type2) (print-color 4) ; function
                    (= (ch :face) :type3) (print-color 5) ; keyword
                    :else (print-color 0))
              (cond (= (ch :bgface) :cursor0) (print-color 10)
                    (= (ch :bgface) :cursor1) (print-color 6)
                    (= (ch :bgface) :cursor2) (print-color 7)
                    (= (ch :bgface) :hl) (print-color 11)
                    (= (ch :bgface) :selection) (print-color 8)
                    (= (ch :bgface) :statusline) (print-color 9)
                    :else (print-color 10))
            )))
        (if (= row (count (first lineslist)))
          (do
            (tty-print (str "  " padding))
            (print-color 0))
          (print-color 10 padding)))
      (swap! old-lines assoc key content))
    ))
  (flush))


(defn view-draw
  []
  (when (empty? @old-lines) (tty-print "\033[0;37m\033[2J"))
  (print-lines (renderer/render-screen)))

(defn view-handler
  [key reference old new]
  (remove-watch editor/editor key)
  (when (editor/fullupdate?) (reset))
  (when (future-done? @updater)
    (dosync (ref-set updater
            (future
              (loop [u @editor/updates]
                (view-draw)
                (when (not= u @editor/updates)
                  (recur @editor/updates)))))))
  (add-watch editor/updates key view-handler))

(defn model-update
  [input]
  (future (editor/handle-input input)))

(defn input-handler
  []
  (future
    (let [r (java.io.BufferedReader. *in*)
          read-input (fn [] (keys/raw2keyword (+ (.read r)
                                                 (if (.ready r) (* 256 (+ (.read r) 1)) 0)
                                                 (if (.ready r) (* 256 256 (+ (.read r) 1)) 0))))]
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