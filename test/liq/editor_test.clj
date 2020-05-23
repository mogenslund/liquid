(ns liq.editor-test
  (:require [clojure.test :refer :all]
            [liq.commands :as commands]
            [liq.modes.fundamental-mode :as fundamental-mode]
            [liq.modes.minibuffer-mode :as minibuffer-mode]
            [liq.modes.clojure-mode :as clojure-mode]
            [liq.modes.spacemacs-mode :as spacemacs-mode]
            [liq.extras.markdownfolds :as markdownfolds]
            [liq.editor :as editor]
            [liq.buffer :refer :all]))

(defn reset-editor
  []
  (let [rows 10
        cols 80]
    (swap! editor/state update ::editor/commands merge commands/commands)
    (editor/add-mode :fundamental-mode fundamental-mode/mode)
    (editor/add-mode :clojure-mode clojure-mode/mode)
    (editor/new-buffer "" {:name "*status-line*" :top rows :left 1 :rows 1 :cols cols
                           :major-modes (list :fundamental-mode) :mode :insert})
    (editor/new-buffer "" {:name "*minibuffer*" :top rows :left 1 :rows 1 :cols cols
                           :major-modes (list :minibuffer-mode) :mode :insert}) 
    (markdownfolds/load-markdownfolds)))

(deftest custom-keybinding-test
  (testing "Resolution of customized keybindings"
    (let [output (atom "")]
      (reset-editor)
      (editor/new-buffer "")
      (swap! editor/state assoc-in [::editor/modes :fundamental-mode :normal "f6"] (fn [] (reset! output "b"))) 
      (swap! editor/state assoc-in [::editor/modes :fundamental-mode :normal "f7"] (fn [] (reset! output "c"))) 
      (swap! editor/state assoc-in [::editor/modes :clojure-mode :normal "f7"] (fn [] (reset! output "d"))) 
      (editor/handle-input "f6")
      (is (= @output "b"))
      (editor/handle-input "f7")
      (is (= @output "d")))))

;; This test will be a great template for general editor interaction
(deftest set-output-handler-test
  (testing "Custom output handler"
    (let [output (atom "")
          printer (fn [b] (reset! output (text b)))
          dimensions (fn [] {:rows 20 :cols 40})
          invalidate (fn [])]
      (reset-editor)
      (editor/set-output-handler
        {:printer printer
         :invalidate invalidate
         :dimensions dimensions})
      (editor/new-buffer "")
      (editor/handle-input "i")
      (editor/handle-input "a")
      (editor/handle-input "b")
      (editor/handle-input "c")
      (editor/paint-buffer)
      (is (= @output "abc")))))

(deftest hidden-text-test
  (testing "Hiding lines"
    (let [output (atom "")
          printer (fn [b] (reset! output (line b))) ; Using printer to retrieve current line
          dimensions (fn [] {:rows 20 :cols 40})
          invalidate (fn [])]
      (reset-editor)
      (editor/set-output-handler
        {:printer printer
         :invalidate invalidate
         :dimensions dimensions})
      (editor/new-buffer "")
      (doseq [c ["i" "#" " " "a" "\n"
                     "b" "c" "\n"
                     "#" " " "d"
                     "esc" "k" "k" "0" "+" "+" "j"]]
        (editor/handle-input c))
      (editor/paint-buffer)
      ;(clojure.pprint/pprint (editor/current-buffer))
      (is (= @output "# d")))))

(deftest dd-with-empty-line
  (testing "Pressing d d i ENTER on line n-1 while line n is empty"
    (let [output (atom "")
          printer (fn [b] (reset! output (text b)))
          dimensions (fn [] {:rows 20 :cols 40})
          invalidate (fn [])]
      (reset-editor)
      (editor/set-output-handler
        {:printer printer
         :invalidate invalidate
         :dimensions dimensions})
      (editor/new-buffer "aa\nbb\n")
      (editor/handle-input "j")
      (editor/handle-input "d")
      (editor/handle-input "d")
      (editor/handle-input "i")
      (editor/handle-input "\n")
      (editor/paint-buffer)
      (is (= @output "aa\n\n")))))

(deftest G-with-empty-line
  (testing "Pressing G i ENTER on line n-1 while line n is empty"
    (let [output (atom "")
          printer (fn [b] (reset! output (text b)))
          dimensions (fn [] {:rows 20 :cols 40})
          invalidate (fn [])]
      (reset-editor)
      (editor/set-output-handler
        {:printer printer
         :invalidate invalidate
         :dimensions dimensions})
      (editor/new-buffer "aa\nbb\n")
      (editor/handle-input "G")
      (editor/handle-input "i")
      (editor/handle-input "\n")
      (editor/paint-buffer)
      (is (= @output "aa\nbb\n\n")))))