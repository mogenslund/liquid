(ns liq.tty-input
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [liq.util :as util]
            [liq.tty-shared :as shared])
  (:import [java.io PrintStream FileOutputStream FileDescriptor]
           [java.io ByteArrayOutputStream BufferedReader]))

(def esc "\033[")
(def ^:private sysout (System/out))

; https://github.com/mogenslund/liquidjs/blob/master/src/dk/salza/liq/adapters/tty.cljs

(defn set-raw-mode
  []
  (shell/sh "/bin/sh" "-c" "stty -echo raw </dev/tty")
  (shared/tty-print esc "0;37m" esc "2J")
  (shared/tty-print esc "?7l")  ; disable line wrap
  (shared/tty-print esc "5000;5000H" esc "s")
  (shared/tty-print esc "K"))

(defn set-line-mode
  []
  ;(tty-print esc "0;37m" esc "2J")
  (shell/sh "/bin/sh" "-c" "stty -echo cooked </dev/tty")
  (shell/sh "/bin/sh" "-c" "stty -echo sane </dev/tty")
  (shared/tty-print esc "0;0H" esc "s"))

(defn exit-handler
  []
  (PrintStream. (FileOutputStream. (FileDescriptor/out)))
  (shared/tty-print "\033[0;37m\033[2J")
  (shared/tty-print "\033[?25h")
  (shell/sh "/bin/sh" "-c" "stty -echo cooked </dev/tty")
  (shell/sh "/bin/sh" "-c" "stty -echo sane </dev/tty")
  (shared/tty-print "\n")
  (System/exit 0))

(defn init
  []
  (shared/tty-print esc "0;0H" esc "s")
  (set-raw-mode))

(defn redirect-stdout
  [fun]
  (System/setOut
    (PrintStream.
      (proxy [ByteArrayOutputStream] []
        (write [& b] (fun (if (= (count b) 1) (str (char (first b))) (str/join "" (map char (first b))))))))))

(defn redirect-stderr
  [fun]
  (System/setErr
    (PrintStream.
      (proxy [ByteArrayOutputStream] []
        (write [& b] (fun (if (= (count b) 1) (str (char (first b))) (str/join "" (map char (first b))))))))))

(defn input-handler
  [fun]
  ;(tty-print esc "0;37m" esc "2J")
  (shared/tty-print esc "0;0H" esc "s")
  (future
    (let [r (BufferedReader. *in*)
          read-input (fn [] (shared/raw2keyword
                              (let [input0 (.read r)]
                                (if (= input0 27)
                                  (loop [res (list input0)]
                                    (Thread/sleep 1)
                                    (if (not (.ready r))
                                      (reverse res)
                                      (recur (conj res (.read r)))))
                                 input0))))]
      (loop [input (read-input)]
        (when (not= input "C-q")
          (fun input)
          (recur (read-input)))))
    (Thread/sleep 30000)
    (shutdown-agents)
    (set-line-mode)))

