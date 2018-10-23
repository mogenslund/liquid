(ns dk.salza.liq.apps.textappwin
  (:require [dk.salza.liq.editor :as editor]
            [dk.salza.liq.apps.promptapp :as promptapp]
            [dk.salza.liq.apps.findfileapp :as findfileapp]
            [dk.salza.liq.extensions.headlinenavigator]
            [dk.salza.liq.extensions.linenavigator]
            [dk.salza.liq.syntaxhl.clojuremdhl :as clojuremdhl]
            [dk.salza.liq.syntaxhl.javascripthl :as javascripthl]
            [dk.salza.liq.syntaxhl.pythonhl :as pythonhl]
            [dk.salza.liq.syntaxhl.xmlhl :as xmlhl]
            [dk.salza.liq.syntaxhl.webassemblyhl :as webassemblyhl]
            [dk.salza.liq.syntaxhl.latexhl :as latexhl]
            [dk.salza.liq.coreutil :refer :all]))

(def keymap (atom {}))

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

(reset! keymap
  {:cursor-color :green
   "\t" #(editor/insert "\t")
   "f5" editor/eval-last-sexp
   "f6" editor/evaluate-file
   "pgdn" #(editor/forward-page)
   "right" #(no-select editor/forward-char)
   "S-right" #(with-select editor/forward-char)
   "left" #(no-select editor/backward-char)
   "S-left" #(with-select editor/backward-char)
   "up" #(no-select editor/backward-line)
   "S-up" #(with-select editor/backward-line)
   "down" #(no-select editor/forward-line)
   "S-down" #(with-select editor/forward-line)
   "home" editor/beginning-of-line
   "end" editor/end-of-line
   "C-home" editor/beginning-of-buffer
   "C-end" editor/end-of-buffer
   "C-c" #(do (editor/copy-selection) (editor/selection-cancel))
   "C-v" editor/paste
   " " #(editor/insert " ")
   "\n" #(editor/insert "\n")
   "C-t" #(editor/insert "\t")
   "C-k" #(do (editor/insert "()") (editor/backward-char)) 
   "C-l" #(do (editor/insert "[]") (editor/backward-char)) 
   "C-h" #(editor/insert "/")
   "backspace" editor/delete
   "C-o" #(findfileapp/run editor/find-file)
   "C-g" editor/escape
   "esc" editor/escape
   "C-w" editor/kill-buffer
   "C-s" editor/save-file
   "C-f" #(promptapp/run editor/find-next '("SEARCH")) ;editor/search}
   :selfinsert editor/insert})


(defn run
  [filepath]
  (if (editor/get-buffer filepath)
    (editor/switch-to-buffer filepath)
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
      (editor/set-keymap @keymap)
      (editor/set-highlighter syntaxhl))))