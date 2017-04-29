(ns dk.salza.liq.extensions.emacsapp
  (:require [dk.salza.liq.editor :as editor]
            [dk.salza.liq.editoractions :as editoractions]
            [dk.salza.liq.keys :as keys]
            [dk.salza.liq.apps.promptapp :as promptapp]
            [dk.salza.liq.apps.commandapp :as commandapp]
            [dk.salza.liq.apps.findfileapp :as findfileapp]
            [dk.salza.liq.extensions.headlinenavigator]
            [dk.salza.liq.syntaxhl.clojuremdhl :as clojuremdhl]
            [dk.salza.liq.syntaxhl.javascripthl :as javascripthl]
            [dk.salza.liq.syntaxhl.pythonhl :as pythonhl]
            [dk.salza.liq.syntaxhl.xmlhl :as xmlhl]
            [dk.salza.liq.syntaxhl.latexhl :as latexhl]
            [dk.salza.liq.coreutil :refer :all]))

(def keymap
  (merge
  {:cursor-color :green
   :space #(editor/insert " ")
   :enter #(editor/insert "\n")
   :tab #(editor/insert "\t")
   :backspace editor/delete
   :C-space editor/selection-toggle
   :C-n editor/forward-line
   :C-p editor/backward-line
   :C-b editor/backward-char
   :C-f editor/forward-char
   :right editor/forward-char
   :left editor/backward-char
   :up editor/backward-line
   :down editor/forward-line
   :C-g editor/escape
   :M-x commandapp/run

   :C-x {:C-f #(findfileapp/run @editor/default-app)
         :C-s editor/save-file
         :C-c editor/quit
         :C-b commandapp/run
         :C-e editor/eval-last-sexp
         :u editor/undo
         :k editor/kill-buffer
        }



   :C-e editor/evaluate-file-raw
   }
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
      (editor/set-keymap keymap)
      (editor/set-highlighter syntaxhl))))

(defn init
  []
  (editor/set-default-app run)
  (editor/set-global-key :C-j #(editor/insert "9"))
  (editor/set-global-key :f5 editor/eval-last-sexp)
  (editor/set-default-keymap keymap))