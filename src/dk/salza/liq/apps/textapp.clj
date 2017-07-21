(ns dk.salza.liq.apps.textapp
  (:require [dk.salza.liq.editor :as editor]
            [dk.salza.liq.editoractions :as editoractions]
            [dk.salza.liq.keys :as keys]
            [dk.salza.liq.apps.promptapp :as promptapp]
            [dk.salza.liq.extensions.headlinenavigator]
            [dk.salza.liq.extensions.linenavigator]
            [dk.salza.liq.syntaxhl.clojuremdhl :as clojuremdhl]
            [dk.salza.liq.syntaxhl.javascripthl :as javascripthl]
            [dk.salza.liq.syntaxhl.pythonhl :as pythonhl]
            [dk.salza.liq.syntaxhl.xmlhl :as xmlhl]
            [dk.salza.liq.syntaxhl.latexhl :as latexhl]
            [dk.salza.liq.coreutil :refer :all]))

(def keymap-insert (atom {}))

(def keymap-navigation
  {:cursor-color :blue
   :tab #(editor/set-keymap @keymap-insert)
   :M editoractions/prompt-to-tmp
   :space #(editor/forward-page)
   ;:C-s editor/search
   :colon (fn [] (editor/handle-input :C-space) (editor/handle-input :colon))
   :right editor/forward-char
   :left editor/backward-char
   :up editor/backward-line
   :down editor/forward-line
   :C-s #(promptapp/run editor/find-next '("SEARCH"))
   :M-s #(promptapp/run editoractions/search-files '("SEARCH"))
   :v editor/selection-toggle
   :g {:g editor/beginning-of-buffer
       :t editor/top-align-page
       :n editor/top-next-headline
       :c #(editor/prompt-append (str "--" (editor/get-context) "--"))
       :i dk.salza.liq.extensions.headlinenavigator/run
       :l dk.salza.liq.extensions.linenavigator/run}
   :dash editor/top-next-headline
   :C-g editor/escape
   :esc editor/escape
   :e editor/eval-last-sexp
   :E editor/evaluate-file
   :C-e editor/evaluate-file-raw
   :l editor/forward-char
   :j editor/backward-char
   :i editor/backward-line
   :k editor/forward-line
   :o (fn [] (do (editor/insert-line) (editor/set-keymap @keymap-insert)))
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
   :1 editor/highlight-sexp-at-point
   :2 editor/select-sexp-at-point
   :y {:y #(do (or (editor/copy-selection) (editor/copy-line)) (editor/selection-cancel))
       :c editor/copy-context}
   :p {:p #(do (editor/insert-line) (editor/paste) (editor/beginning-of-line))
       :h editor/paste}
   :d {:d #(do (or (editor/delete-selection) (editor/delete-line)) (editor/selection-cancel))}
   :s editor/save-file
   :u editor/undo
   :C-w editor/kill-buffer
   :C-t (fn [] (editor/tmp-test))
   })

(reset! keymap-insert
  (merge
    {:cursor-color :green
     :tab #(editor/set-keymap keymap-navigation)
     :right editor/forward-char
     :left editor/backward-char
     :up editor/backward-line
     :down editor/forward-line
     :space #(editor/insert " ")
     :enter #(editor/insert "\n")
     :C-t #(editor/insert "\t")
     :backspace editor/delete
     :C-g editor/escape
     :esc editor/escape
     :C-w editor/kill-buffer
     :C-s #(promptapp/run editor/find-next '("SEARCH"))} ;editor/search}
    (keys/alphanum-mapping editor/insert)
    (keys/symbols-mapping editor/insert)))

(defn run
  [filepath]
  (if (editor/get-buffer filepath)
    (editor/switch-to-buffer filepath)
    (let [syntaxhl (cond (nil? filepath) clojuremdhl/next-face
                         (re-matches #"^.*\.js$" filepath) javascripthl/next-face
                         (re-matches #"^.*\.java$" filepath) javascripthl/next-face
                         (re-matches #"^.*\.c$" filepath) javascripthl/next-face
                         (re-matches #"^.*\.py$" filepath) pythonhl/next-face
                         (re-matches #"^.*\.xml$" filepath) xmlhl/next-face
                         (re-matches #"^.*\.tex$" filepath) latexhl/next-face
                          :else clojuremdhl/next-face) ;; In other cases use clojure/markdown
          ]
      (editor/create-buffer-from-file filepath)
      (editor/set-highlighter syntaxhl))))