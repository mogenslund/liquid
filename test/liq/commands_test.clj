(ns liq.commands-test
  (:require [clojure.test :refer :all]
            [liq.buffer :as buffer]
            [liq.commands :refer :all]))

(deftest get-namespace-test
  (testing "Get namespace"
    (is (nil? (get-namespace (buffer/buffer ""))))
    (is (= (get-namespace (buffer/buffer "(ns abc.def)\n  asdf")) "abc.def"))
    (is (= (get-namespace (buffer/buffer "(ns abc.def\n  asdf)")) "abc.def"))))

