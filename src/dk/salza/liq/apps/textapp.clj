(ns dk.salza.liq.apps.textapp
  (:require [dk.salza.liq.editor :as editor]
            [dk.salza.liq.slider :refer :all]
            [dk.salza.liq.apps.promptapp :as promptapp]
            [dk.salza.liq.extensions.headlinenavigator]
            [dk.salza.liq.extensions.linenavigator]
            [dk.salza.liq.extensions.folding :as folding]
            [dk.salza.liq.syntaxhl.clojuremdhl :as clojuremdhl]
            [dk.salza.liq.syntaxhl.javascripthl :as javascripthl]
            [dk.salza.liq.syntaxhl.pythonhl :as pythonhl]
            [dk.salza.liq.syntaxhl.xmlhl :as xmlhl]
            [dk.salza.liq.syntaxhl.webassemblyhl :as webassemblyhl]
            [dk.salza.liq.syntaxhl.latexhl :as latexhl]
            [dk.salza.liq.coreutil :refer :all]))

(def keymap-insert (atom {}))
(def keymap-navigation (atom {}))

(defn set-navigation-key
  [key fun]
  (swap! keymap-navigation assoc key fun))

(defn set-insert-key
  [key fun]
  (swap! keymap-insert assoc key fun))

(reset! keymap-navigation {:cursor-color :blue
   "\t" #(editor/set-keymap @keymap-insert)
   "M" editor/prompt-to-tmp
   " " #(editor/forward-page)
   "pgdn" #(editor/forward-page)
   ;:C-s editor/search
   ":" (fn [] (editor/handle-input :C-space) (editor/handle-input :colon))
   "right" editor/forward-char
   "left" editor/backward-char
   "up" editor/backward-line
   "down" editor/forward-line
   "home" editor/beginning-of-line
   "end" editor/end-of-line
   "C-s" #(promptapp/run editor/find-next '("SEARCH"))
   "M-s" #(promptapp/run editor/search-files '("SEARCH"))
   "v" editor/selection-toggle
   "g" {:info (str "g: Beginning of buffer\nt: Top align\nn: Top next headline\n"
                  "c: Show context\ni: Navigate headlines\nl: Navigate lines")
       "g" editor/beginning-of-buffer
       "t" editor/top-align-page
       "n" editor/top-next-headline
       "c" #(editor/prompt-append (str "--" (editor/get-context) "--"))
       "i" dk.salza.liq.extensions.headlinenavigator/run
       "l" dk.salza.liq.extensions.linenavigator/run}
   "-" editor/top-next-headline
   "C-g" editor/escape
   "esc" editor/escape
   "e" editor/eval-last-sexp
   "E" editor/evaluate-file
   "C-e" editor/evaluate-file-raw
   "l" editor/forward-char
   "j" editor/backward-char
   "i" editor/backward-line
   "k" editor/forward-line
   "o" (fn [] (do (editor/insert-line) (editor/set-keymap @keymap-insert)))
   "J" editor/beginning-of-line
   "G" editor/end-of-buffer
   "L" editor/end-of-line
   "x" editor/delete-char
   "m" editor/previous-real-buffer 
   "h" editor/run-macro
   "H" editor/record-macro
   "n" editor/find-next
   "O" editor/context-action
   "w" editor/forward-word
   "b" editor/backward-word
   "K" editor/swap-line-down
   "I" editor/swap-line-up
   "r" {" " #(editor/replace-char " ")
        :selfinsert editor/replace-char}
   "1" editor/highlight-sexp-at-point
   "2" editor/select-sexp-at-point
   "y" {:info "y: Line or selection\nc: Context\nf: Current filepath"
       "y" #(do (or (editor/copy-selection) (editor/copy-line)) (editor/selection-cancel))
       "c" editor/copy-context
       "f" editor/copy-file}
   "p" {:info "p: Paste new line\nh: Paste here"
       "p" #(do (editor/insert-line) (editor/paste) (editor/beginning-of-line))
       "h" editor/paste}
   "d" {:info "d: Line or selection"
       "d" #(do (or (editor/delete-selection) (editor/delete-line)) (editor/selection-cancel))}
   "s" editor/save-file
   "u" editor/undo
   "C-w" editor/kill-buffer
   "+" {"+" #(editor/apply-to-slider folding/cycle-level-fold)
        "0" #(editor/apply-to-slider folding/expand-all)
        "1" #(editor/apply-to-slider (fn [sl] (folding/collapse-all (folding/fold-all-def sl))))
        "2" #(editor/apply-to-slider (fn [sl] (folding/unfold-all-level sl 2)))
        "3" #(editor/apply-to-slider (fn [sl] (folding/unfold-all-level sl 3)))
        "4" #(editor/apply-to-slider (fn [sl] (folding/unfold-all-level sl 4)))
        "5" #(editor/apply-to-slider (fn [sl] (folding/unfold-all-level sl 5)))
        "s" #(if (editor/selection-active?) (do (editor/hide-selection) (editor/selection-cancel)) (editor/unhide))
        "f" #(editor/apply-to-slider folding/fold-def)}
   "C-t" (fn [] (editor/tmp-test))
   })

(reset! keymap-insert
  {:cursor-color :green
   "\t" #(editor/set-keymap @keymap-navigation)
   "pgdn" #(editor/forward-page)
   "right" editor/forward-char
   "left" editor/backward-char
   "up" editor/backward-line
   "down" editor/forward-line
   "home" editor/beginning-of-line
   "end" editor/end-of-line
   " " #(editor/insert " ")
   "\n" #(editor/insert "\n")
   "C-t" #(editor/insert "\t")
   "C-k" #(do (editor/insert "()") (editor/backward-char)) 
   "C-l" #(do (editor/insert "[]") (editor/backward-char)) 
   "C-h" #(editor/insert "/")
   "backspace" editor/delete
   "C-g" editor/escape
   "esc" editor/escape
   "C-w" editor/kill-buffer
   "C-s" #(promptapp/run editor/find-next '("SEARCH")) ;editor/search}
   :selfinsert editor/insert})

(defn run
  [filepath]
  (if (editor/get-buffer filepath)
    (editor/switch-to-buffer-same-window filepath)
    (let [syntaxhl (cond (nil? filepath) (editor/get-default-highlighter)
                         (re-matches #"^.*\.js$" filepath) javascripthl/next-face
                         (re-matches #"^.*\.java$" filepath) javascripthl/next-face
                         (re-matches #"^.*\.c$" filepath) javascripthl/next-face
                         (re-matches #"^.*\.py$" filepath) pythonhl/next-face
                         (re-matches #"^.*\.xml$" filepath) xmlhl/next-face
                         (re-matches #"^.*\.wat$" filepath) webassemblyhl/next-face
                         (re-matches #"^.*\.tex$" filepath) latexhl/next-face
                          :else (editor/get-default-highlighter)) ;; In other cases use clojure/markdown
          ]
      (editor/create-buffer-from-file filepath)
      (editor/set-keymap @keymap-navigation)
      (editor/set-highlighter syntaxhl))))
