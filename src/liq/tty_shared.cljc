(ns liq.tty-shared
  (:require [clojure.string :as str]
            #?(:clj [clojure.java.io :as io]))
  #?(:clj (:import [java.io PrintStream BufferedOutputStream FileOutputStream FileDescriptor])))

#?(:clj (def out0 (PrintStream. (BufferedOutputStream. (FileOutputStream. (FileDescriptor/out))))))

(defn tty-print
  [& args]
  #?(:bb (binding [*out* (io/writer System/out)] (print (str/join "" args)))
     :clj (.print out0 (str/join "" args))
     :cljs (js/process.stdout.write (str/join "" args))))

(defn tty-println
  [& args]
  #?(:bb (binding [*out* (io/writer System/out)] (println (str/join "" args)))
     :clj (.println out0 (str/join "" args))
     :cljs (js/process.stdout.write (str (str/join "" args) "\n"))))

(defn flush-output
  []
  (.flush out0))

(defn raw2keyword
  [raw]
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
            (= raw2 '(27 91 51 126)) "delete"
            (= raw2 '(27 79 80)) "f1"
            (= raw2 '(27 79 81)) "f2"
            (= raw2 '(27 79 82)) "f3"
            (= raw2 '(27 79 83)) "f4"
            (= raw2 '(27 91 49 53 126)) "f5"
            (= raw2 '(27 91 49 55 126)) "f6"
            (= raw2 '(27 91 49 56 126)) "f7"
            (= raw2 '(27 91 49 57 126)) "f8"
            (= raw2 '(27 91 50 48 126)) "f9"
            (= raw2 '(27 91 50 49 126)) "f10"
            (= raw2 '(27 91 50 51 126)) "f11"
            (= raw2 '(27 91 50 52 126)) "f12"
            (= raw2 '(27 91 49 59 50 65)) "S-up"
            (= raw2 '(27 91 49 59 50 66)) "S-down"
            (= raw2 '(27 91 49 59 50 68)) "S-left"
            (= raw2 '(27 91 49 59 50 67)) "S-right"
            (= raw2 '(27 91 49 59 53 65)) "C-up"
            (= raw2 '(27 91 49 59 53 66)) "C-down"
            (= raw2 '(27 91 49 59 53 68)) "C-left"
            (= raw2 '(27 91 49 59 53 67)) "C-right"
            (= raw2 '(27 91 49 59 53 72)) "C-home"
            (= raw2 '(27 91 49 59 53 70)) "C-end"
            true (str (char c0))))))

