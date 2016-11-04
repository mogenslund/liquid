(ns dk.salza.liq.editoractions
  "This namespace is for actions that more or
  less can be composed of more atomic editor
  functions."
  (:require [dk.salza.liq.editor :as editor]
            [dk.salza.liq.coreutil :refer :all]
            [clojure.string :as str]))

(defn swap-windows
  []
  (let [buffer1 (editor/get-name)]
    (editor/switch-to-buffer "scratch")
    (editor/other-window)
    (let [buffer2 (editor/get-name)]
      (editor/switch-to-buffer buffer1)
      (editor/other-window)
      (editor/switch-to-buffer buffer2))))

(defn prompt-to-tmp
  []
  (editor/switch-to-buffer "-prompt-")
  (let [content (editor/get-content)]
    (editor/other-window)
    (editor/new-buffer "-tmp-")
    (editor/clear)
    (editor/insert content)))