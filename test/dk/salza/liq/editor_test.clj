(ns dk.salza.liq.editor-test
  (:require [clojure.test :refer :all]
            [dk.salza.liq.editor :as editor]))

(deftest basic-editor-test
  (testing "Creating editor and simple buffer insert"
    (editor/new-buffer "somebuffer")
    (editor/insert "abc")
    (is (= (editor/get-content) "abc"))
    (editor/insert "xyz")
    (is (= (editor/get-content) "abcxyz"))))