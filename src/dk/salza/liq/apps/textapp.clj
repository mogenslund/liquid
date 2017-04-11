(ns dk.salza.liq.apps.textapp
  (:require [dk.salza.liq.editor :as editor]
            [dk.salza.liq.modes.textmode :as textmode]
            [dk.salza.liq.syntaxhl.clojuremdhl :as clojuremdhl]
            [dk.salza.liq.syntaxhl.javascripthl :as javascripthl]
            [dk.salza.liq.syntaxhl.pythonhl :as pythonhl]
            [dk.salza.liq.syntaxhl.xmlhl :as xmlhl]
            [dk.salza.liq.syntaxhl.latexhl :as latexhl]
            [dk.salza.liq.coreutil :refer :all]))

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
          mode (textmode/create syntaxhl)]
      (editor/create-buffer-from-file filepath)
      (editor/set-mode mode))))