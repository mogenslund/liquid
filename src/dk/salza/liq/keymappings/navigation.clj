(ns dk.salza.liq.keymappings.navigation
  (:require [dk.salza.liq.editor :as editor]
            [dk.salza.liq.slider :refer :all]
            [dk.salza.liq.extensions.headlinenavigator]
            [dk.salza.liq.extensions.linenavigator]
            [dk.salza.liq.extensions.folding :as folding]
            [dk.salza.liq.apps.promptapp :as promptapp]))

(def keymapping
  {:id "dk.salza.liq.keymappings.navigation"
   :cursor-color :blue
   "\t" #(editor/set-keymap "dk.salza.liq.keymappings.insert")
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
   "esc" #(editor/set-keymap "dk.salza.liq.keymappings.normal")
   "e" editor/eval-last-sexp
   "E" editor/evaluate-file
   "C-e" editor/evaluate-file-raw
   "l" editor/forward-char
   "j" editor/backward-char
   "i" editor/backward-line
   "k" editor/forward-line
   "o" (fn [] (do (editor/insert-line) (editor/set-keymap "dk.salza.liq.keymappings.insert")))
   "J" editor/beginning-of-line
   "G" editor/end-of-buffer
   "L" editor/end-of-line
   "x" editor/delete-char
   "delete" editor/delete-char
   "m" editor/previous-real-buffer 
   "h" editor/run-macro
   "H" editor/record-macro
   "n" editor/find-next
   "O" editor/context-action
   "w" editor/forward-word
   "b" editor/backward-word
   "C-j" editor/swap-line-down
   "C-k" editor/swap-line-up
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