(ns dk.salza.liq.keys
  (:require [dk.salza.liq.logging :as logging]))

;; http://ascii-table.com/ansi-escape-sequences.php
(defn raw2keyword
  [raw]
  (logging/log "RAW" raw)
  (let [k (str (char (min raw 400)))]
     (cond (re-matches #"[a-zA-Z0-9]" k) (keyword k)
           (= raw 9) :tab
           (= raw 32) :space
           (= raw 13) :enter
           (= raw 27) :esc
           (= raw 33) :exclamation
           (= raw 34) :quote
           (= raw 35) :hash
           (= raw 36) :dollar
           (= raw 37) :percent
           (= raw 38) :ampersand
           (= raw 39) :singlequote
           (= raw 40) :parenstart
           (= raw 41) :parenend
           (= raw 42) :asterisk
           (= raw 43) :plus
           (= raw 44) :comma
           (= raw 45) :dash
           (= raw 46) :dot
           (= raw 47) :slash
           (= raw 58) :colon
           (= raw 59) :semicolon
           (= raw 60) :lt
           (= raw 61) :equal
           (= raw 62) :gt
           (= raw 63) :question
           (= raw 64) :at
           (= raw 91) :bracketstart
           (= raw 92) :backslash
           (= raw 93) :bracketend
           (= raw 94) :hat
           (= raw 95) :underscore
           (= raw 4348955) :up           
           (= raw 4545563) :left           
           (= raw 4414491) :down           
           (= raw 4480027) :right           
           (= raw 4807707) :home
           (= raw 4676635) :end
           (= raw 123) :bracesstart
           (= raw 124) :pipe
           (= raw 125) :bracesend
           (= raw 126) :tilde
           (= raw 164) :curren
           (= raw 180) :backtick
           (= raw 197) :caa
           (= raw 198) :cae
           (= raw 216) :coe
           (= raw 229) :aa
           (= raw 230) :ae
           (= raw 248) :oe
           (= raw 1) :C-a
           (= raw 2) :C-b
           (= raw 3) :C-c
           (= raw 4) :C-d
           (= raw 5) :C-e
           (= raw 6) :C-f
           (= raw 7) :C-g
           (= raw 8) :C-h
           (= raw 10) :C-j
           (= raw 11) :C-k
           (= raw 12) :C-l
           (= raw 14) :C-n
           (= raw 15) :C-o
           (= raw 16) :C-p
           (= raw 17) :C-q
           (= raw 18) :C-r
           (= raw 19) :C-s
           (= raw 20) :C-t
           (= raw 21) :C-u
           (= raw 22) :C-v
           (= raw 23) :C-w
           (= raw 24) :C-x
           (= raw 25) :C-y
           (= raw 26) :C-z
           (= raw 4635) :C-M-q
           (= raw 0) :C-space
           (= raw 127) :backspace
           (= raw 3611) :M-enter
           (= raw 25115) :M-a
           (= raw 25371) :M-b
           (= raw 25627) :M-c
           (= raw 29723) :M-s
           (= raw 31003) :M-x
           (= raw 5394459) :f2
           (= raw 5459995) :f3
           (= raw 5525531) :f4
           (= raw 32565) :f5
           (= raw 32567) :f6
           (= raw 32568) :f7
           (= raw 32569) :f8
           (= raw 32560) :f9
           (= raw 32561) :f10
           ;(= raw ) :f11
           (= raw 32564) :f12
           true (keyword char))))

(defn alphanum-mapping
  [fun]
  (into {}
        (map (fn [x] [(keyword (str (char x)))
                      #(fun (str (char x)))])
             (concat (range 97 123) (range 65 91) (range 48 58)))))

(defn lower-mapping
  [fun]
  (into {}
        (map (fn [x] [(keyword (str (char x)))
                      #(fun (str (char x)))])
             (range 97 123))))

(defn symbols-mapping
  [fun]
  {:exclamation #(fun "!")
   :quote #(fun "\"")
   :hash #(fun "#")
   :dollar #(fun "$")
   :percent #(fun "%")
   :ampersand #(fun "&")
   :singlequote #(fun "'")
   :parenstart #(fun "(")
   :parenend #(fun ")")
   :asterisk #(fun "*")
   :plus #(fun "+")
   :comma #(fun ",")
   :dash #(fun "-")
   :dot #(fun ".")
   :slash #(fun "/")
   :colon #(fun ":")
   :semicolon #(fun ";")
   :lt #(fun "<")
   :equal #(fun "=")
   :gt #(fun ">")
   :question #(fun "?")
   :at #(fun "@")
   :bracketstart #(fun "[")
   :bracketend #(fun "]")
   :hat #(fun "^")
   :bracesstart #(fun "{")
   :underscore #(fun "_")
   :backslash #(fun "\\")
   :pipe #(fun "|")
   :bracesend #(fun "}")
   :tilde #(fun "~")
   :curren #(fun "¤")
   :backtick #(fun "´")
   :caa #(fun "Å")
   :cae #(fun "Æ")
   :coe #(fun "Ø")
   :aa #(fun "å")
   :ae #(fun "æ")
   :oe #(fun "ø")})