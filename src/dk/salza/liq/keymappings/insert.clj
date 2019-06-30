(ns dk.salza.liq.keymappings.insert
  (:require [dk.salza.liq.editor :as editor]
            [dk.salza.liq.slider :refer :all]
            [dk.salza.liq.apps.promptapp :as promptapp]))

(defn with-select
  [action]
  (when (not (editor/selection-active?))
    (editor/selection-toggle))
  (action))

(defn no-select
  [action]
  (when (editor/selection-active?)
    (editor/selection-toggle))
  (action))

(def keymapping
  {:id "dk.salza.liq.keymappings.insert"
   :cursor-color :green
   ;"\t" #(editor/set-keymap "dk.salza.liq.keymappings.navigation")
   ;"\t" #(editor/set-keymap "dk.salza.liq.keymappings.normal")
   "f5" editor/eval-last-sexp
   "f6" editor/evaluate-file
   "pgdn" #(editor/forward-page)
   "right" editor/forward-char
   "S-right" #(with-select editor/forward-char)
   "left" editor/backward-char
   "S-left" #(with-select editor/backward-char)
   "up" editor/backward-line
   "S-up" #(with-select editor/backward-line)
   "down" editor/forward-line
   "S-down" #(with-select editor/forward-line)
   "home" editor/beginning-of-line
   "end" editor/end-of-line
   "C-home" editor/beginning-of-buffer
   "C-end" editor/end-of-buffer
   "C-c" #(do (editor/copy-selection) (editor/selection-cancel))
   "C-x" #(do (editor/delete-selection) (editor/selection-cancel))
   "C-v" editor/paste
   " " #(editor/insert " ")
   "\n" #(editor/insert "\n")
   "C-t" #(editor/insert "\t")
   "C-k" #(do (editor/insert "()") (editor/backward-char)) 
   "C-l" #(do (editor/insert "[]") (editor/backward-char)) 
   "C-h" #(editor/insert "/")
   "C-n" editor/forward-line
   "C-p" editor/backward-line
   "backspace" editor/delete
   "delete" editor/delete-char
   "C-g" editor/escape
   "esc" #(editor/set-keymap "dk.salza.liq.keymappings.normal")
   "C-w" editor/kill-buffer
   "C-s" #(promptapp/run editor/find-next '("SEARCH")) ;editor/search}
   :selfinsert editor/insert})
