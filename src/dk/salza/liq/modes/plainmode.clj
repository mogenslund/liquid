(ns dk.salza.liq.modes.plainmode
  (:require [dk.salza.liq.editor :as editor]
            [dk.salza.liq.editoractions :as editoractions]
            [dk.salza.liq.mode :as mode]
            [dk.salza.liq.keys :as keys]
            [dk.salza.liq.modes.findfilemode :as findfilemode]
            [dk.salza.liq.modes.commandmode :as commandmode]
            [dk.salza.liq.modes.promptmode :as promptmode]
            [dk.salza.liq.extensions.headlinenavigator]
            [dk.salza.liq.coreutil :refer :all]))

(def mode
  (-> (mode/create "plainmode")
      (mode/set-highlighter #(str ">> " "" %))
      (mode/set-actions
        (merge
          {:cursor-color :green
           :tab editor/swap-actionmapping
           :C-o editor/other-window
           :C-space commandmode/run
           :C-f #(findfilemode/run editor/find-file)
           :right editor/forward-char
           :left editor/backward-char
           :up editor/backward-line
           :down editor/forward-line
           :space #(editor/insert " ")
           :enter #(editor/insert "\n")
           :C-t #(editor/insert "\t")
           :backspace editor/delete
           :C-g editor/escape
           :C-w editor/kill-buffer
           :C-s #(promptmode/run editor/find-next '("SEARCH"))} ;editor/search}
          (keys/alphanum-mapping editor/insert)
          (keys/symbols-mapping editor/insert)))

      (mode/swap-actionmapping)
      (mode/set-actions
        {:cursor-color :blue
         :tab editor/swap-actionmapping
         :C-o editor/other-window
         :M editoractions/prompt-to-tmp
         :C-f #(findfilemode/run editor/find-file)
         :C-space commandmode/run
         :space #(editor/forward-page)
         ;:C-s editor/search
         :right editor/forward-char
         :left editor/backward-char
         :up editor/backward-line
         :down editor/forward-line
         :C-s #(promptmode/run editor/find-next '("SEARCH"))
         :v editor/selection-toggle
         :g {:g editor/beginning-of-buffer
             :t editor/top-align-page
             :i dk.salza.liq.extensions.headlinenavigator/run}
         :C-g editor/escape
         :e editor/eval-last-sexp
         :E editor/evaluate-file
         :C-e editor/evaluate-file-raw
         :l editor/forward-char
         :j editor/backward-char
         :i editor/backward-line
         :k editor/forward-line
         :o (fn [] (do (editor/insert-line) (editor/swap-actionmapping)))
         :J editor/beginning-of-line
         :G editor/end-of-buffer
         :L editor/end-of-line
         :x editor/delete-char
         :m editor/previous-real-buffer 
         :h editor/run-macro
         :H editor/record-macro
         :n editor/find-next
         :O editor/context-action
         :w editor/forward-word
         :K editor/swap-line-down
         :I editor/swap-line-up
         :r (merge {:space #(editor/replace-char " ")}
                   (keys/alphanum-mapping editor/replace-char)
                   (keys/symbols-mapping editor/replace-char))
         :1 editor/select-sexp-at-point
         :y {:y #(do (or (editor/copy-selection) (editor/copy-line)) (editor/selection-cancel))}
         :p {:p #(do (editor/insert-line) (editor/paste) (editor/beginning-of-line))
             :h editor/paste}
         :d {:d #(do (or (editor/delete-selection) (editor/delete-line)) (editor/selection-cancel))}
         :s editor/save-file
         :u editor/undo
         :C-w editor/kill-buffer
         :C-t editor/tmp-test
         ;:C-t #(promptmode/run str '("a" "tadaa"))
         })))
  