(ns dk.salza.liq.reproduce-test
  "Tests for reproducing known bugs"
  (:require [clojure.test :refer :all]
            [dk.salza.liq.editor :as editor]
            [dk.salza.liq.apps.textapp :as textapp]
            [dk.salza.liq.window :as window]
            [dk.salza.liq.core :as core]))

;; lein test :only dk.salza.liq.reproduce-test/eval-last-sexp-interactively
(deftest eval-last-sexp-interactively
  (testing "That calling (eval-last-sexp) on
           an interactive function like
           (editor/end-of-buffer) will not freeze
           the program"
    (editor/set-default-keymap @textapp/keymap-navigation)
    (editor/add-window (window/create "prompt" 1 1 30 40 "-prompt-"))
    (editor/new-buffer "-prompt-")
    (editor/add-window (window/create "main" 1 44 30 (- 100 46) "scratch")) ; todo: Change to percent given by setting. Not hard numbers
    (editor/new-buffer "scratch")
    (editor/insert (str "(editor/end-of-buffer)"))
    (editor/end-of-buffer))
    (editor/handle-input "j")
    (editor/handle-input "j")
    (editor/handle-input "j")
    (editor/handle-input "j")
    (is (= 18 (editor/get-point)))
    (editor/handle-input "j")
    (editor/handle-input "j")
    (is (= 16 (editor/get-point))))