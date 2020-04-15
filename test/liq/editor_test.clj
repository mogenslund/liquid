(ns liq.editor-test
  (:require [clojure.test :refer :all]
            [liq.commands :as commands]
            [liq.modes.fundamental-mode :as fundamental-mode]
            [liq.modes.minibuffer-mode :as minibuffer-mode]
            [liq.modes.clojure-mode :as clojure-mode]
            [liq.modes.spacemacs-mode :as spacemacs-mode]
            [liq.editor :as editor]
            [liq.buffer :refer :all]))

(deftest custom-keybinding-test
  (testing "Resolution of customized keybindings"
    (let [output (atom "")
          rows 10
          cols 80]
      (swap! editor/state update ::editor/commands merge commands/commands)
      (editor/add-mode :fundamental-mode fundamental-mode/mode)
      (editor/add-mode :clojure-mode clojure-mode/mode)
      (editor/new-buffer "" {:name "*status-line*" :top rows :left 1 :rows 1 :cols cols
                             :major-modes (list :fundamental-mode) :mode :insert})
      (editor/new-buffer "" {:name "*minibuffer*" :top rows :left 1 :rows 1 :cols cols
                             :major-modes (list :minibuffer-mode) :mode :insert})
      (editor/new-buffer "a" {:name "abc"})
      (swap! editor/state assoc-in [::editor/modes :fundamental-mode :normal "f6"] (fn [] (reset! output "b"))) 
      (swap! editor/state assoc-in [::editor/modes :fundamental-mode :normal "f7"] (fn [] (reset! output "c"))) 
      (swap! editor/state assoc-in [::editor/modes :clojure-mode :normal "f7"] (fn [] (reset! output "d"))) 
      (editor/handle-input "f6")
      (is (= @output "b"))
      (editor/handle-input "f7")
      (is (= @output "d")))))