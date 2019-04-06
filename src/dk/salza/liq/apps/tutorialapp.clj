(ns dk.salza.liq.apps.tutorialapp
  (:require [clojure.java.io :as io]
            [dk.salza.liq.editor :as editor]))

(defn run
  []
  (if (nil? (editor/get-buffer "-tutorial-"))
    (do
      (editor/new-buffer "-tutorial-")
      (editor/insert (slurp (io/resource "tutorial.md")))
      (editor/insert "Tadaaa")
      (editor/set-global-key "f5" run))
    (editor/switch-to-buffer "-tutorial-")))
