(ns dk.salza.liq.keymappings.normal
  (:require [dk.salza.liq.editor :as editor]
            [dk.salza.liq.slider :refer :all]
            [dk.salza.liq.extensions.headlinenavigator]
            [dk.salza.liq.extensions.linenavigator]
            [dk.salza.liq.extensions.folding :as folding]
            [dk.salza.liq.apps.promptapp :as promptapp]))

(def basic-mappings
  {:id "dk.salza.liq.keymappings.normal"
   "M" editor/prompt-to-tmp
   " " #(editor/forward-page)
   ;"C-s" editor/search
   ":" (fn [] (editor/handle-input "C- ") (editor/handle-input ":"))
   "right" editor/forward-char
   "left" editor/backward-char
   "up" editor/backward-line
   "down" editor/forward-line
   "C-s" #(promptapp/run editor/find-next '("SEARCH"))
   "v" editor/selection-toggle
   "g" {"g" editor/beginning-of-buffer
       "t" editor/top-align-page
       "n" editor/top-next-headline
       "c" #(editor/prompt-append (str "--" (editor/get-context) "--"))
       "i" dk.salza.liq.extensions.headlinenavigator/run}
   "dash" editor/top-next-headline
   "C-g" editor/escape
   "e" editor/end-of-word
   "E" editor/evaluate-file
   "C-e" editor/evaluate-file-raw
   "l" editor/forward-char
   "h" editor/backward-char
   "k" editor/backward-line
   "j" editor/forward-line
   "o" #(do (editor/insert-line) (editor/set-keymap "dk.salza.liq.keymappings.insert"))
   "a" #(do (editor/forward-char) (editor/set-keymap "dk.salza.liq.keymappings.insert"))
   "A" #(do (editor/end-of-line) (editor/set-keymap "dk.salza.liq.keymappings.insert"))
   "i" #(editor/set-keymap "dk.salza.liq.keymappings.insert")
   "0" editor/beginning-of-line
   "J" editor/beginning-of-line
   "G" editor/end-of-buffer
   "$" editor/end-of-line
   "L" editor/end-of-line
   "x" editor/delete-char
   "m" editor/previous-real-buffer 
   "h-" editor/run-macro
   "H" editor/record-macro
   "n" editor/find-next
   "O" editor/context-action
   "w" editor/forward-word
   "K" editor/swap-line-down
   "I" #(do (editor/beginning-of-line) (editor/set-keymap "dk.salza.liq.keymappings.insert"))
   "r" {" " #(editor/replace-char " ")
        :selfinsert editor/replace-char}
   "1" editor/select-sexp-at-point
   "y" (fn []
        (if (editor/copy-selection)
          (editor/selection-cancel)
          {"y" editor/copy-line}))
   "p" {"p" #(do (editor/insert-line) (editor/paste) (editor/beginning-of-line))
       "h" editor/paste}
   "d" {"d" #(do (or (editor/delete-selection) (editor/delete-line)) (editor/selection-cancel))}
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
   "c" {"p" {"p" editor/eval-last-sexp
             "f" editor/evaluate-file}}
   "C-t" editor/tmp-test })

(defn keymapping
  [keyw]
  (cond (= keyw "2") #(editor/prompt-append "--2--")
        true (basic-mappings keyw)))