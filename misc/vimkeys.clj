(ns dk.salza.liq.extensions.vimkeys
  (:require [dk.salza.liq.editor :as editor]
            [dk.salza.liq.keys :as keys]
            [dk.salza.liq.apps.promptapp :as promptapp]
            [dk.salza.liq.extensions.headlinenavigator]
            [dk.salza.liq.syntaxhl.clojuremdhl :as clojuremdhl]
            [dk.salza.liq.syntaxhl.javascripthl :as javascripthl]
            [dk.salza.liq.syntaxhl.pythonhl :as pythonhl]
            [dk.salza.liq.syntaxhl.xmlhl :as xmlhl]
            [dk.salza.liq.syntaxhl.latexhl :as latexhl]
            [dk.salza.liq.coreutil :refer :all]))

;; https://vim.rtorr.com/
(def keymap-insert (atom {}))

(def keymap-normal
  {:cursor-color :blue
   :M editor/prompt-to-tmp
   :space #(editor/forward-page)
   ;:C-s editor/search
   :colon (fn [] (editor/handle-input :C-space) (editor/handle-input :colon))
   :right editor/forward-char
   :left editor/backward-char
   :up editor/backward-line
   :down editor/forward-line
   :C-s #(promptapp/run editor/find-next '("SEARCH"))
   :v editor/selection-toggle
   :g {:g editor/beginning-of-buffer
       :t editor/top-align-page
       :n editor/top-next-headline
       :c #(editor/prompt-append (str "--" (editor/get-context) "--"))
       :i dk.salza.liq.extensions.headlinenavigator/run}
   :dash editor/top-next-headline
   :C-g editor/escape
   :e editor/end-of-word
   :E editor/evaluate-file
   :C-e editor/evaluate-file-raw
   :l editor/forward-char
   :h editor/backward-char
   :k editor/backward-line
   :j editor/forward-line
   :o #(do (editor/insert-line) (editor/set-keymap @keymap-insert))
   :a #(do (editor/forward-char) (editor/set-keymap @keymap-insert))
   :A #(do (editor/end-of-line) (editor/set-keymap @keymap-insert))
   :i #(editor/set-keymap @keymap-insert)
   :0 editor/beginning-of-line
   :J editor/beginning-of-line
   :G editor/end-of-buffer
   :dollar editor/end-of-line
   :L editor/end-of-line
   :x editor/delete-char
   :m editor/previous-real-buffer 
   :h- editor/run-macro
   :H editor/record-macro
   :n editor/find-next
   :O editor/context-action
   :w editor/forward-word
   :K editor/swap-line-down
   :I #(do (editor/beginning-of-line) (editor/set-keymap @keymap-insert))
   :r (merge {:space #(editor/replace-char " ")}
             (keys/alphanum-mapping editor/replace-char)
             (keys/symbols-mapping editor/replace-char))
   :1 editor/select-sexp-at-point
   :y (fn []
        (if (editor/copy-selection) (editor/selection-cancel)
          (reset! editor/submap
          {:y editor/copy-line})))
   :p {:p #(do (editor/insert-line) (editor/paste) (editor/beginning-of-line))
       :h editor/paste}
   :d {:d #(do (or (editor/delete-selection) (editor/delete-line)) (editor/selection-cancel))}
   :s editor/save-file
         :u editor/undo
   :C-w editor/kill-buffer
   :C-t editor/tmp-test
   ;:C-t #(promptapp/run str '("a" "tadaa"))
   })

(reset! keymap-insert
  (merge
    {:cursor-color :green
     :esc #(do (editor/set-keymap keymap-normal) (editor/backward-char) (when (= (editor/get-char) "\n") (editor/forward-char)))
     :right editor/forward-char
     :left editor/backward-char
     :up editor/backward-line
     :down editor/forward-line
     :space #(editor/insert " ")
     :enter #(editor/insert "\n")
     :tab #(editor/insert "\t")
     :backspace editor/delete
     :C-g editor/escape
     :C-w editor/kill-buffer
     :C-s #(promptapp/run editor/find-next '("SEARCH"))} ;editor/search}
    (keys/alphanum-mapping editor/insert)
    (keys/symbols-mapping editor/insert)))

(defn init
  []
  (editor/set-global-key :C-j #(editor/insert "9"))
  (editor/set-global-key :f5 editor/eval-last-sexp)
  (editor/set-default-keymap keymap-normal))