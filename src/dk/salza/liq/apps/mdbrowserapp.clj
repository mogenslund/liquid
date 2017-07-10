(ns dk.salza.liq.apps.mdbrowserapp
  (:require [clojure.java.io :as io]
            [dk.salza.liq.editor :as editor]
            [dk.salza.liq.editoractions :as editoractions]
            [dk.salza.liq.keys :as keys]
            [dk.salza.liq.apps.promptapp :as promptapp]
            [dk.salza.liq.extensions.headlinenavigator]
            [dk.salza.liq.extensions.linenavigator]
            [dk.salza.liq.syntaxhl.clojuremdhl :as clojuremdhl]
            [dk.salza.liq.coreutil :refer :all]))

(def navigate (atom nil))

(def keymap
  {:cursor-color :blue
   :M editoractions/prompt-to-tmp
   :enter #(@navigate)
   :tab #(editor/find-next "[")
   :space #(editor/forward-page)
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
   :J editor/beginning-of-line
   :G editor/end-of-buffer
   :L editor/end-of-line
   :m editor/previous-real-buffer 
   :n editor/find-next
   :O editor/context-action
   :w editor/forward-word
   :1 editor/highlight-sexp-at-point
   :2 editor/select-sexp-at-point
   :y {:y #(do (or (editor/copy-selection) (editor/copy-line)) (editor/selection-cancel))}
   :C-w editor/kill-buffer
   :C-t (fn [] (editor/tmp-test))
   })

;; Links:
;; [direct.md]
;;
;; [My Link][1]
;; [1]: Path to link

;; :enter should open link in mdbrowserapp
;; :tab Move cursor to next link
(defn run
  "TODO: Implementation not finished yet.
  App to browse files written in markdown.
  TODO: Support browsing webpages by converting to
  markdown."
  [filepath]
  (if (editor/get-buffer filepath)
    (editor/switch-to-buffer filepath)
    (do
      (editor/create-buffer-from-file filepath)
      (editor/set-keymap keymap)
      (editor/set-highlighter clojuremdhl/next-face))))

(reset! navigate
  (fn []
    (let [context (editor/get-context)
          type (context :type)
          value (context :value)]
      (cond (= type :file) (if (.isAbsolute (io/file value))
                             (run value)
                             (run (str (io/file (editor/get-folder) value))))
            (= type :url) (when-let [start-browser (editor/setting :start-browser)]
                            (start-browser value))
            (= type :function) (editor/goto-definition value)))))