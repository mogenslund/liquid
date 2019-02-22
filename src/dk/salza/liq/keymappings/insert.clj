(ns dk.salza.liq.keymappings.insert
  (:require [dk.salza.liq.editor :as editor]
            [dk.salza.liq.slider :refer :all]
            [dk.salza.liq.apps.promptapp :as promptapp]))

(def keymapping
  {:id "dk.salza.liq.keymappings.insert"
   :cursor-color :green
   ;"\t" #(editor/set-keymap "dk.salza.liq.keymappings.navigation")
   "\t" #(editor/set-keymap "dk.salza.liq.keymappings.normal")
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
   "delete" editor/delete-char
   "C-g" editor/escape
   "esc" #(editor/set-keymap "dk.salza.liq.keymappings.normal")
   "C-w" editor/kill-buffer
   "C-s" #(promptapp/run editor/find-next '("SEARCH")) ;editor/search}
   :selfinsert editor/insert})