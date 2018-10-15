(ns dk.salza.liq.buffer-test
  (:require [clojure.test :refer :all]
            [dk.salza.liq.tools.fileutil :as fileutil]
            [dk.salza.liq.slider :as slider]
            [dk.salza.liq.buffer :refer :all]))

(deftest create-from-file-test
  (testing "Creating buffer from file"
    (let [path (fileutil/tmp-file "createfromfiletest.txt")]
      (spit path "abc\ndef")
      (is (= (-> (create-from-file path) get-slider slider/get-content) "abc\ndef")))))

