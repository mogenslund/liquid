(ns dk.salza.liq.apps.textapp
  (:require [dk.salza.liq.editor :as editor]
            [dk.salza.liq.slider :refer :all]
            [dk.salza.liq.keymappings.navigation]
            [dk.salza.liq.keymappings.normal]
            [dk.salza.liq.keymappings.insert]
            [dk.salza.liq.apps.promptapp :as promptapp]
            [dk.salza.liq.extensions.headlinenavigator]
            [dk.salza.liq.extensions.linenavigator]
            [dk.salza.liq.extensions.folding :as folding]
            [dk.salza.liq.syntaxhl.clojuremdhl :as clojuremdhl]
            [dk.salza.liq.syntaxhl.javascripthl :as javascripthl]
            [dk.salza.liq.syntaxhl.pythonhl :as pythonhl]
            [dk.salza.liq.syntaxhl.xmlhl :as xmlhl]
            [dk.salza.liq.syntaxhl.webassemblyhl :as webassemblyhl]
            [dk.salza.liq.syntaxhl.latexhl :as latexhl]
            [dk.salza.liq.coreutil :refer :all]))

(def keymap-insert (atom {}))
(def keymap-navigation (atom {}))
(def keymap-normal (atom {}))

(defn set-navigation-key
  [key fun]
  (swap! keymap-navigation assoc key fun))

(defn set-insert-key
  [key fun]
  (swap! keymap-insert assoc key fun))

(reset! keymap-navigation
  (assoc dk.salza.liq.keymappings.navigation/keymapping
    "\t" #(editor/set-keymap @keymap-insert)
     "o" (fn [] (do (editor/insert-line) (editor/set-keymap @keymap-insert)))))

(reset! keymap-normal
  (assoc dk.salza.liq.keymappings.normal/keymapping
   "o" #(do (editor/insert-line) (editor/set-keymap @keymap-insert))
   "a" #(do (editor/forward-char) (editor/set-keymap @keymap-insert))
   "A" #(do (editor/end-of-line) (editor/set-keymap @keymap-insert))
   "I" #(do (editor/beginning-of-line) (editor/set-keymap @keymap-insert))
   "i" #(editor/set-keymap @keymap-insert)))

(reset! keymap-insert
  (assoc dk.salza.liq.keymappings.insert/keymapping
    "esc" #(editor/set-keymap @keymap-normal)
    "\t" #(editor/set-keymap @keymap-navigation)))

(defn run
  [filepath]
  (if (editor/get-buffer filepath)
    (editor/switch-to-buffer-same-window filepath)
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
      (editor/set-keymap @keymap-navigation)
      (editor/set-highlighter syntaxhl))))
