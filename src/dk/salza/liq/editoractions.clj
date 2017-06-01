(ns dk.salza.liq.editoractions
  "This namespace is for actions that more or
  less can be composed of more atomic editor
  functions."
  (:require [dk.salza.liq.editor :as editor]
            [dk.salza.liq.coreutil :refer :all]
            [dk.salza.liq.tools.cshell :as cshell]
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

(defn search-files
  [search]
  (editor/new-buffer "-search-files-")
  (editor/kill-buffer)
  (when (> (count (editor/get-folder)) 1) ;; Avoid searching from empty or root folder
    (let [folder (editor/get-folder)
          result (->> folder
                      cshell/lsr
                      (cshell/flrex (re-pattern search))
                      (map #(str/join " : " %))
                      (str/join "\n"))]
      (editor/new-buffer "-search-files-")
      (editor/insert result)
  ;(->> (get-folder) lsr (flrex (re-pattern search)) (map #(str/join " : " %)) p)
  )))