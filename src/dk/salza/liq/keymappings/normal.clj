(ns dk.salza.liq.keymappings.normal
  (:require [dk.salza.liq.editor :as editor]
            [dk.salza.liq.slider :refer :all]
            [dk.salza.liq.apps.commandapp :as commandapp]
            [dk.salza.liq.extensions.headlinenavigator]
            [dk.salza.liq.extensions.linenavigator]
            [dk.salza.liq.extensions.folding :as folding]
            [dk.salza.liq.apps.promptapp :as promptapp]))

(def motion-repeat (atom 0))

(defn reset-motion-repeat
  []
  (reset! motion-repeat 0))

(defn enlarge-motion-repeat
  [n]
  (swap! motion-repeat #(min (+ (* 10 %) n) 300)))

(defn motion-repeat-fun
  [fun]
  (fn []
    (dotimes [n (max @motion-repeat 1)] (fun))
    (reset-motion-repeat)))

(def keymapping ; basic-mappings
  {:id "dk.salza.liq.keymappings.normal"
   :after-hook (fn [k] (when (not (re-find #"\d" k)) (reset-motion-repeat)))
   "0" #(if (= @motion-repeat 0) (editor/beginning-of-line) (enlarge-motion-repeat 0)) 
   "1" #(enlarge-motion-repeat 1)
   "2" #(enlarge-motion-repeat 2)
   "3" #(enlarge-motion-repeat 3)
   "4" #(enlarge-motion-repeat 4)
   "5" #(enlarge-motion-repeat 5)
   "6" #(enlarge-motion-repeat 6)
   "7" #(enlarge-motion-repeat 7)
   "8" #(enlarge-motion-repeat 8)
   "9" #(enlarge-motion-repeat 9)
   "M" editor/prompt-to-tmp
   " " (motion-repeat-fun editor/forward-page)
   ;"C-s" editor/search
   ":" #(do (editor/request-fullupdate) (commandapp/run ":i :"))
   "right" (motion-repeat-fun editor/forward-char)
   "left" (motion-repeat-fun editor/backward-char)
   "up" (motion-repeat-fun editor/backward-line)
   "down" (motion-repeat-fun editor/forward-line)
   "C-s" #(promptapp/run editor/find-next '("SEARCH"))
   "v" editor/selection-toggle
   "g" {"g" editor/beginning-of-buffer
       "t" editor/top-align-page
       "n" editor/top-next-headline
       "c" #(editor/prompt-append (str "--" (editor/get-context) "--"))
       "i" dk.salza.liq.extensions.headlinenavigator/run}
   "dash" editor/top-next-headline
   "C-g" #(do (editor/escape) (reset-motion-repeat))
   "e" (motion-repeat-fun editor/end-of-word)
   "E" editor/evaluate-file
   "C-e" editor/evaluate-file-raw
   "l" (motion-repeat-fun editor/forward-char)
   "h" (motion-repeat-fun editor/backward-char)
   "k" (motion-repeat-fun editor/backward-line)
   "j" (motion-repeat-fun editor/forward-line)
   "o" #(do (editor/insert-line) (editor/set-keymap "dk.salza.liq.keymappings.insert"))
   "a" #(do (editor/forward-char) (editor/set-keymap "dk.salza.liq.keymappings.insert"))
   "A" #(do (editor/end-of-line) (editor/set-keymap "dk.salza.liq.keymappings.insert"))
   "i" #(editor/set-keymap "dk.salza.liq.keymappings.insert")
   "J" editor/beginning-of-line
   "G" editor/end-of-buffer
   "$" editor/end-of-line
   "L" editor/end-of-line
   "x" (motion-repeat-fun editor/delete-char)
   "m" editor/previous-real-buffer 
   "q" editor/run-macro
   "Q" editor/record-macro
   "n" editor/find-next
   "O" editor/context-action
   "w" (motion-repeat-fun editor/forward-word)
   "C-j" (motion-repeat-fun editor/swap-line-down)
   "C-k" (motion-repeat-fun editor/swap-line-up)
   "I" #(do (editor/beginning-of-line) (editor/set-keymap "dk.salza.liq.keymappings.insert"))
   "r" {" " #(editor/replace-char " ")
        :selfinsert editor/replace-char}
   "y" {:info "y: Line or selection\nc: Context\nf: Current filepath"
       "y" #(do (or (editor/copy-selection) (editor/copy-line)) (editor/selection-cancel))
       "c" editor/copy-context
       "f" editor/copy-file}
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
   "," {"," editor/highlight-sexp-at-point
        "s" editor/select-sexp-at-point}
   "c" {"p" {"p" editor/eval-last-sexp
             "f" editor/evaluate-file}}
   "C-t" editor/tmp-test })

