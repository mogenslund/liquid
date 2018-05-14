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

(deftest backward-word-test
  (let [b0 (-> (create "-test-")
               (insert  "abc      def\nhij")
               end-of-buffer)
        b1 (backward-word b0)
        b2 (backward-word b1)
        b3 (backward-word b2)]
    (testing "Backward word basic cases"
      (is (= (get-char b1) "h"))
      (is (= (get-char b2) "d"))
      (is (= (get-char b3) "a")))))
