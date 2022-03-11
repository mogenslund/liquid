(ns liq.modes.fundamental-mode
  (:require [clojure.string :as str]
            [liq.editor :as editor :refer [apply-to-buffer switch-to-buffer get-buffer]]
            [liq.buffer :as buffer :refer [delete-region shrink-region set-insert-mode]]
            [liq.commands :as c]
            [liq.util :as util]))

(defn non-repeat-fun
  [fun]
  (when (not= (@editor/state ::editor/repeat-counter) 0) (swap! editor/state assoc ::editor/repeat-counter 0))
  (editor/apply-to-buffer fun))

(def Emacs-C-x
 {"C-e" :eval-sexp-at-point
  "C-f" :Ex
  "C-s" :w
  "C-c" :q
  "C-b" #(((editor/get-mode :buffer-chooser-mode) :init))})

(def fd-chord-timestamp (atom 0))

(defn ^:buffer fd-chord-f
  [buf]
  (reset! fd-chord-timestamp (System/currentTimeMillis))
  (buffer/insert-char buf "f"))

(defn ^:buffer fd-chord-d
  [buf]
  (if (and (= (buffer/get-char (buffer/left buf)) "f") (< (- (System/currentTimeMillis) @fd-chord-timestamp) 100))
    (-> buf buffer/delete-backward buffer/set-normal-mode)
    (buffer/insert-char buf "d")))

(def mode
  {:commands {":ts" #(editor/message (str % " -- " (buffer/sexp-at-point (editor/current-buffer))))}
   :insert {"esc" (fn [] (apply-to-buffer #(buffer/left (buffer/set-normal-mode %))))
            "backspace" #(non-repeat-fun buffer/delete-backward)
            ;; Emacs
            "f" #'fd-chord-f
            "d" #'fd-chord-d
            "C-b" :left
            "C-n" :down
            "C-p" :up
            "C-f" :right
            "left" :left 
            "down" :down 
            "up" :up 
            "right" :right 
            "pgdn" :page-down
            "pgup" :page-up
            "home" :beginning-of-line
            "end" :end-of-line
            "delete" :delete-char
            "C-x" Emacs-C-x 
            "M-x" (fn [] (when (not= (@editor/state ::editor/repeat-counter) 0) (swap! editor/state assoc ::editor/repeat-counter 0))
                       (((editor/get-mode :minibuffer-mode) :init) ":"))}
   :normal {"esc" (fn []
                    (when (not= (@editor/state ::editor/repeat-counter) 0) (swap! editor/state assoc ::editor/repeat-counter 0))
                    (editor/paint-all-buffer-groups))
            "C- " #(((editor/get-mode :buffer-chooser-mode) :init))
            "C-b" :previous-regular-buffer
            "C-f" :page-down
            "C-o" :output-snapshot
            "C-x" Emacs-C-x
            "t" {"a" #(editor/message "Test A")
                 "b" (with-meta #(editor/message "Test B") {:doc "Some documentation for Test B"})
                 "c" :testc
                 "d" :testd
                 "e" (with-meta (fn [buf]
                                    (future (Thread/sleep 1000) (editor/message "Test E: Using with-meta inline"))
                                    (buffer/down buf))
                                {:buffer true :doc "Some documentation for Test E"})}
                 
            "f2" editor/oldest-buffer
            "f3" #(non-repeat-fun buffer/debug-clear-undo)
            "." ::editor/last-action 
            "0" :0 
            "1" :1 
            "2" :2 
            "3" :3 
            "4" :4 
            "5" :5 
            "6" :6 
            "7" :7
            "8" :8
            "9" :9 
            "%" :move-matching-paren
            "i" :set-insert-mode 
            "a" :insert-after-point
            "I" :insert-at-beginning-of-line
            "h" :left 
            "j" :down 
            "k" :up 
            "l" :right 
            "left" :left 
            "down" :down 
            "up" :up 
            "right" :right 
            "pgdn" :page-down
            "pgup" :page-up
            "home" :beginning-of-line
            "end" :end-of-line
            "delete" :delete-char
            "w" :word-forward
            "W" :word-forward-ws
            "b" :beginning-of-word
            "e" :end-of-word
            "E" :end-of-word-ws
            "^" :first-non-blank
            "$" :end-of-line
            "x" :delete-char
            "v" :set-visual-mode
            "n" :search
            "u" :undo
            "y" {"y" :copy-line
                 "%" :yank-filename
                 "i" {"w" :yank-inner-word
                      "(" :yank-inner-paren
                      "[" :yank-inner-bracket
                      "{" :yank-inner-brace
                      "\"" :yank-inner-quote}
                 "a" {"(" :yank-outer-paren
                      "[" :yank-outer-bracket
                      "{" :yank-outer-brace
                      "\"" :yank-outer-quote}}
            "p" :paste-clipboard
            "P" :paste-clipboard-here
            "g" {"g" :beginning-of-buffer
                 "i" :navigate-definitions
                 "l" :navigate-lines
                 "f" :open-file-at-point
                 "J" :join-lines}
            "G" :end-of-buffer
            "z" {"t" :scroll-cursor-top 
                 "\n" :scroll-cursor-top}
            "d" {"d" :delete-line
                 "w" :delete-to-word
                 "e" :delete-to-end-of-word
                 "E" :delete-to-end-of-word-ws
                 "i" {"w" :delete-inner-word
                      "(" :delete-inner-paren
                      "[" :delete-inner-bracket
                      "{" :delete-inner-brace
                      "\"" :delete-inner-quote}
                 "a" {"(" :delete-outer-paren
                      "[" :delete-outer-bracket
                      "{" :delete-outer-brace
                      "\"" :delete-outer-quote}}
            "A" :insert-at-line-end
            "D" :delete-to-line-end
            "r" {:selfinsert (fn [buf c]
                               (when (not= (@editor/state ::editor/repeat-counter) 0) (swap! editor/state assoc ::editor/repeat-counter 0))
                               (buffer/set-char buf (first c)))}
            "c" {"p" {"p" :eval-sexp-at-point
                      "r" :raw-eval-sexp-at-point
                      "f" :eval-buffer
                      "F" :evaluate-file-raw}
                 "i" {"w" :change-inner-word
                      "(" :change-inner-paren
                      "[" :change-inner-bracket
                      "{" :change-inner-brace
                      "\"" :change-inner-quote}
                 "a" {"(" :change-outer-paren
                      "[" :change-outer-bracket
                      "{" :change-outer-brace
                      "\"" :change-outer-quote}
                 "c" :change-line
                 "$" :change-eol
                 "e" :change-to-end-of-word
                 "E" :change-to-end-of-word-ws
                 "w" :change-to-end-of-word}
            "C" :change-eol
            "/" (fn [] (when (not= (@editor/state ::editor/repeat-counter) 0) (swap! editor/state assoc ::editor/repeat-counter 0))
                    (((editor/get-mode :minibuffer-mode) :init) "/"))
            "f" {:selfinsert buffer/search} 
            ":" (fn [] (when (not= (@editor/state ::editor/repeat-counter) 0) (swap! editor/state assoc ::editor/repeat-counter 0))
                       (((editor/get-mode :minibuffer-mode) :init) ":")) 
            "Q" editor/record-macro
            "q" editor/run-macro
            "J" :join-lines-space
            "V" :select-line
            "o" :append-line
            "O" :append-line-above
            "C-w" {"-" :window-smaller
                   "+" :window-larger
                   "<" :window-narrower
                   ">" :window-wider
                   "j" :window-below
                   "k" :window-above
                   "h" :window-left
                   "l" :window-right}}
    :visual {"esc" :set-normal-mode 
             "i" {"w" :select-inner-word
                  "(" :select-inner-paren
                  "[" :select-inner-bracket
                  "{" :select-inner-brace
                  "\"" :select-inner-quote}
             "a" {"(" :select-outer-paren
                  "[" :select-outer-bracket
                  "{" :select-outer-brace
                  "\"" :select-outer-quote}
             "V" :select-line
             "c" :change
             "y" :copy-selection-to-clipboard
             "d" :delete}}) 
             
