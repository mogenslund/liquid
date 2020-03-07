(ns liq2.tty-input
  (:require [clojure.string :as str]
            [liq2.tty-shared :as shared]))

(def esc "\033[")

(defn- tty-print
  [& args]
  (js/process.stdout.write (str/join "" args)))

(defn set-raw-mode
  []
  (let [readline (js/require "readline")]
    (.emitKeypressEvents readline process.stdin
      (js/process.stdin.setRawMode true)))
  (tty-print esc "0;37m" esc "2J"))

(def translate-name
  {"return" "\n"
   "space" ""
   "escape" "esc"})

(defn jsx->clj
  [x]
  (into {} (for [k (.keys js/Object x)] [k (aget x k)])))

(defn exit-handler
  []
  (tty-print "\033[0;37m\033[2J")
  (tty-print "\033[?25h")
  (let [readline (js/require "readline")]
    (.emitKeypressEvents readline process.stdin
      (js/process.stdin.setRawMode false)))
  (tty-print "\n")
  (js/process.exit 0))

(defn init
  []
  (tty-print esc "0;0H" esc "s")
  (set-raw-mode))

(defn input-handler
  [fun]
  (set-raw-mode)
  ;(tty-print esc "0;37m" esc "2J")
  (tty-print esc "0;0H" esc "s")
  (js/process.stdin.on "keypress"
    (fn [chunk key]
      (let [k (or (translate-name js/key.name)
                  (str (when js/key.ctrl "C-")
                     (when js/key.meta "M-")
                     (cond (= (count js/key.sequence) 1) js/key.sequence
                           js/key.shift (str/upper-case js/key.name)
                           true js/key.name)))] 
        (if (not= k "C-q")
          (fun k)
          (js/process.exit))))))
