(ns dk.salza.liq.apps.textapp
  (:require [dk.salza.liq.editor :as editor]
            [dk.salza.liq.modes.textmode :as textmode]
            [dk.salza.liq.syntaxhl.clojuremdhl :as clojuremdhl]
            [dk.salza.liq.coreutil :refer :all]))

(defn run
  [filepath]
  (if (editor/get-buffer filepath)
    (editor/switch-to-buffer filepath)
    (let [mode (textmode/create clojuremdhl/next-face)]
      (editor/create-buffer-from-file filepath)
      (editor/set-mode mode))))
