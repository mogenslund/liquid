(ns dk.salza.liq.buffer-test
  (:require [clojure.test :refer :all]
            [dk.salza.liq.tools.fileutil :as fileutil]
            [dk.salza.liq.buffer :refer :all]))

(deftest insert-test
  (testing "Inserting a string"
    (is (= (-> (create "mybuffer")
               (insert "abcdef")
               get-visible-content)
           "abcdef"))))


(deftest create-from-file-test
  (testing "Creating buffer from file"
    (let [path (fileutil/tmp-file "createfromfiletest.txt")]
      (spit path "abc\ndef")
      (is (= (-> (create-from-file path) (forward-char 2) get-char)
             (-> (create path) (insert "abc\ndef") (backward-char 5) get-char))))))