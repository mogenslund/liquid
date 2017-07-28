(ns dk.salza.liq.apps.helpapp
  (:require [clojure.java.io :as io]
            [dk.salza.liq.editor :as editor]
            [dk.salza.liq.apps.typeaheadapp :as typeaheadapp]
            [dk.salza.liq.apps.promptapp :as promptapp]
            [dk.salza.liq.buffer :as buffer]
            [dk.salza.liq.slider :refer :all]
            [dk.salza.liq.keys :as keys]
            [dk.salza.liq.extensions.headlinenavigator]
            [dk.salza.liq.extensions.linenavigator]
            [dk.salza.liq.syntaxhl.clojuremdhl :as clojuremdhl]
            [dk.salza.liq.logging :as logging]
            [dk.salza.liq.coreutil :refer :all]
            [clojure.string :as str]))

(def navigate (atom nil))

(def keymap
  {:cursor-color :blue
   :M editor/prompt-to-tmp
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

(defn help-function
  []
  (typeaheadapp/run (str/split-lines (with-out-str (clojure.repl/dir editor)))
                    str #(editor/prompt-set (with-out-str (clojure.repl/find-doc %)))))
;  (editor/prompt-set (str "Not implemented yet"
;    (with-out-str (clojure.repl/dir editor))
;    (with-out-str (clojure.repl/doc editor/doto-buffer)))))

(defn help-apropos
  []
  (let [context (re-find #"[^/]*$" ((editor/get-context) :value))
        fd (fn
             [s]
             
             (if (empty? s)
               (clojure.repl/find-doc context)
               (clojure.repl/find-doc s)))]
    (promptapp/run fd (list (str "APROPOS (" context ")")))))

(defn help-key
  []
  (str "Not implemented yet"))

(defn help-browse
  [topic]
  (let [buffername (str "*HELP* " topic)]
  (if (editor/get-buffer buffername)
    (editor/switch-to-buffer buffername)
    (do
      (editor/new-buffer buffername)
      (editor/set-keymap keymap)
      (editor/set-highlighter clojuremdhl/next-face)
      (editor/insert (slurp (io/resource (str "help/" topic))))
      (editor/beginning-of-buffer)))))

(reset! navigate
  (fn []
    (let [topic ((editor/get-context) :value)]
      (when (re-matches #"[-a-z0-9]+\.md" topic) 
        (help-browse topic)))))