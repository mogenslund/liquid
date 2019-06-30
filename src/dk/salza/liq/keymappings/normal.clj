(ns dk.salza.liq.keymappings.normal
  (:require [dk.salza.liq.editor :as editor]
            [dk.salza.liq.slider :refer :all]
            [dk.salza.liq.apps.commandapp :as commandapp]
            [dk.salza.liq.apps.findfileapp :as findfileapp]
            [dk.salza.liq.apps.textapp :as textapp]
            [dk.salza.liq.apps.keynavigateapp :as keynavigateapp]
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

(defn wrap-selection [sl p1 p2]
  (let [m1 (get-mark sl "selection")
        m2 (get-point sl)]
    (if m1
      (-> sl
          (remove-mark "selection")
          (set-point (max m1 m2))
          (insert p2)
          (set-point (min m1 m2))
          (insert p1)
          (set-point m2))
      sl)))

(defn change
  [fun]
  (fn []
    (editor/selection-set)
    (fun)
    (editor/delete-selection)
    (editor/selection-cancel)
    (editor/set-keymap "dk.salza.liq.keymappings.insert")))

(defn insert-mode
  []
  (editor/set-keymap "dk.salza.liq.keymappings.insert"))

; https://github.com/emacs-evil/evil/blob/3766a521a60e6fb0073220199425de478de759ad/evil-maps.el
(def keymapping ; basic-mappings
  {:id "dk.salza.liq.keymappings.normal"
   :after-hook (fn [k] (when (not (re-find #"[\dc]" k)) (reset-motion-repeat)))
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
   " " #(keynavigateapp/run (editor/get-spacemap))
   "C-f" (motion-repeat-fun editor/forward-page)
   ":" #(do (editor/request-fullupdate) (commandapp/run ":i :"))
   "(" #(wrap-selection % "(" ")") ;)
   "[" #(wrap-selection % "[" "]")
   "{" #(wrap-selection % "{" "}")
   "right" (motion-repeat-fun editor/forward-char)
   "left" (motion-repeat-fun editor/backward-char)
   "up" (motion-repeat-fun editor/backward-line)
   "down" (motion-repeat-fun editor/forward-line)
   "/" #(promptapp/run editor/find-next '("/"))
   "-" #(promptapp/run editor/find-next '("/"))
   "?" #(promptapp/run editor/find-prev '("?"))
   "M-s" #(promptapp/run editor/search-files '("SEARCH"))
   "v" editor/selection-toggle
   "g" {"g" editor/beginning-of-buffer
        "f" editor/context-action
        "t" editor/top-align-page
        "n" editor/top-next-headline
        "c" #(editor/prompt-append (str "--" (editor/get-context) "--"))
        "i" dk.salza.liq.extensions.headlinenavigator/run
        "l" dk.salza.liq.extensions.linenavigator/run}
   "dash" editor/top-next-headline
   "C-g" #(do (editor/escape) (reset-motion-repeat))
   "e" (motion-repeat-fun editor/end-of-word)
   "E" (motion-repeat-fun editor/end-of-word2)
   "C-e" editor/evaluate-file-raw
   "l" (motion-repeat-fun editor/forward-char)
   "h" (motion-repeat-fun editor/backward-char)
   "k" (motion-repeat-fun editor/backward-line)
   "j" (motion-repeat-fun editor/forward-line)
   "o" #(do (editor/insert-line) (insert-mode))
   "a" #(do (editor/forward-char) (insert-mode))
   "A" #(do (editor/end-of-line) (insert-mode))
   "i" insert-mode
   "J" editor/join-lines
   "^" editor/first-non-blank
   "G" editor/end-of-buffer
   "$" editor/end-of-line
   "L" editor/end-of-line
   "x" (motion-repeat-fun editor/delete-char)
   "m" editor/previous-real-buffer
   "q" editor/run-macro
   "Q" editor/record-macro
   "n" editor/find-continue
   "N" editor/find-continue-opposite
   "O" #(do (editor/insert-line-above) (insert-mode))
   "w" (motion-repeat-fun editor/forward-word)
   "W" (motion-repeat-fun editor/forward-word2)
   "b" (motion-repeat-fun editor/backward-word)
   "B" (motion-repeat-fun editor/backward-word2)
   "C-j" (motion-repeat-fun editor/swap-line-down)
   "C-k" (motion-repeat-fun editor/swap-line-up)
   "I" #(do (editor/first-non-blank) (insert-mode))
   "r" {" " #(editor/replace-char " ")
        :selfinsert editor/replace-char}
   "f" {:selfinsert (fn [c] (editor/find-next c))}
   "F" {:selfinsert editor/find-char-previous}
   "y" {:info "y: Line or selection\nc: Context\nf: Current filepath"
        :direct-condition #(editor/selection-active?)
        :direct-action #(do (editor/copy-selection) (editor/selection-cancel))
       "y" editor/copy-line
       "c" editor/copy-context
       "f" editor/copy-file}
   "Y" editor/copy-line
   "p" {"p" editor/paste-after
       "h" editor/paste}
   "P" editor/paste-before
   "d" {:direct-condition #(editor/selection-active?)
        :direct-action #(do (editor/delete-selection) (editor/selection-cancel))
        "d" editor/delete-line
        "e" #(do
               (editor/selection-set)
               (editor/end-of-word)
               (editor/delete-selection)
               (editor/selection-cancel))
        "w" #(do
               (editor/selection-set)
               (editor/forward-word)
               (editor/delete-selection)
               (editor/selection-cancel))}
   "D" editor/delete-to-end-of-line
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
  ; "," #(keynavigateapp/run ((editor/get-spacemap) "m"))
   "," {"," editor/highlight-sexp-at-point
        "s" editor/select-sexp-at-point
        "g" editor/context-action}
   "c" {"p" {"p" editor/eval-last-sexp
             "f" editor/evaluate-file}
        "e" (change (motion-repeat-fun editor/end-of-word))
        "E" (change (motion-repeat-fun editor/end-of-word2))
        "l" (change (motion-repeat-fun editor/forward-char))
        "h" (change (motion-repeat-fun editor/backward-char))
        "k" (change (motion-repeat-fun editor/backward-line))
        "j" (change (motion-repeat-fun editor/forward-line))
        "w" (change (motion-repeat-fun editor/forward-word))
        "W" (change (motion-repeat-fun editor/forward-word2))}
   "C-t" editor/tmp-test })

