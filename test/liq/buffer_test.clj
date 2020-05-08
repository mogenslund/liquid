(ns liq.buffer-test
  (:require [clojure.test :refer :all]
            [liq.buffer :refer :all]))

(deftest insert-in-vector-test
  (testing "Insert into vector"
    (is (= (insert-in-vector [1 2 3] 0 9) [9 1 2 3]))
    (is (= (insert-in-vector [1 2 3] 1 9) [1 9 2 3]))
    (is (= (insert-in-vector [1 2 3] 3 9) [1 2 3 9]))))

(deftest remove-from-vector-test
  (testing "Remove from vector, single"
    (is (= (remove-from-vector [1 2 3] 0) [1 2 3]))
    (is (= (remove-from-vector [1 2 3] 1) [2 3]))
    (is (= (remove-from-vector [1 2 3] 2) [1 3]))
    (is (= (remove-from-vector [1 2 3] 3) [1 2])))
  (testing "Remove from vector, multi"
    (is (= (remove-from-vector [1 2 3 4] 2 3) [1 4]))
    (is (= (remove-from-vector [1 2 3 4] 2 2) [1 3 4]))
    (is (= (remove-from-vector [1 2 3 4] 1 4) []))))
    

(deftest sub-buffer-test
  (testing "Sub buffer"
    (is (= (-> (buffer "aaa\nbbb\nccc\nddd\neee") (sub-buffer 2 3) text)
           "bbb\nccc"))
    (is (= (-> (buffer "aaa\nbbb\nccc\nddd\neee") (sub-buffer 1 3) text)
           "aaa\nbbb\nccc"))
    (is (= (-> (buffer "aaa\nbbb\nccc\nddd\neee") down down down right (sub-buffer 2 4) :liq.buffer/cursor :liq.buffer/row)
           3))
    (is (= (-> (buffer "aaa\nbbb\nccc\nddd\neee") down down down right (sub-buffer 1 4) :liq.buffer/cursor :liq.buffer/row)
           4))))

(deftest line-count-test
  (testing "Line count"
    (is (= (-> (buffer "") line-count) 1)) 
    (is (= (-> (buffer "abc") line-count) 1)) 
    (is (= (-> (buffer "abc\n") line-count) 2)) 
    (is (= (-> (buffer "abc  \n  abc") line-count) 2)) 
    (is (= (-> (buffer "abc\n\n") line-count) 3)) 
    (is (= (-> (buffer "abc\n\nabc") line-count) 3)) 
    (is (= (-> (buffer "abc\n\n") end-of-buffer line-count) 3)) 
    (is (= (-> (buffer "abc\n\nabc") end-of-buffer line-count) 3))))

(deftest col-count-test
  (testing "Col count"
    (is (= (-> (buffer "") (col-count 1)) 0)) 
    (is (= (-> (buffer "a") (col-count 1)) 1)) 
    (is (= (-> (buffer "a\n") (col-count 1)) 1)) 
    (is (= (-> (buffer "a\n") (col-count 2)) 0)) 
    (is (= (-> (buffer "a\n") (col-count 10)) 0)) 
    (is (= (-> (buffer "a\nabc") (col-count 2)) 3))))


(deftest right-test
  (testing "Forward char"
    (let [buf (buffer "abc\n\ndef")]
      (is (= (buf :liq.buffer/cursor) {:liq.buffer/row 1 :liq.buffer/col 1}))
      (is (= (-> buf
                 right
                 :liq.buffer/cursor)
             {:liq.buffer/row 1 :liq.buffer/col 2}))
      (is (= (-> buf
                 (right 10)
                 :liq.buffer/cursor)
             {:liq.buffer/row 1 :liq.buffer/col 3}))
      (is (= (-> buf
                 down
                 right
                 :liq.buffer/cursor)
             {:liq.buffer/row 2 :liq.buffer/col 1})))))

(deftest point-compare-test
  (testing "Point compare"
    (is (= (point-compare {:liq.buffer/row 1 :liq.buffer/col 1} {:liq.buffer/row 1 :liq.buffer/col 2}) -1))
    (is (= (point-compare {:liq.buffer/row 1 :liq.buffer/col 10} {:liq.buffer/row 1 :liq.buffer/col 2}) 1))
    (is (= (point-compare {:liq.buffer/row 2 :liq.buffer/col 1} {:liq.buffer/row 1 :liq.buffer/col 2}) 1))
    (is (= (point-compare {:liq.buffer/row 1 :liq.buffer/col 2} {:liq.buffer/row 1 :liq.buffer/col 2}) 0))))

(deftest line-test
  (testing "Get line"
    (is (= (-> (buffer "") line) ""))
    (is (= (-> (buffer "aaa") (line 1)) "aaa"))
    (is (= (-> (buffer "aaa") (line 2)) ""))
    (is (= (-> (buffer "\naaa") (line 2)) "aaa"))))

(deftest text-test
  (testing "Get text"
    (let [buf (buffer "abc\n\ndef")]
      (is (= (text buf) "abc\n\ndef"))
      (is (= (text buf {:liq.buffer/row 1 :liq.buffer/col 1} {:liq.buffer/row 1 :liq.buffer/col 10}) "abc"))
      (is (= (text buf {:liq.buffer/row 1 :liq.buffer/col 1} {:liq.buffer/row 1 :liq.buffer/col 3}) "abc"))
      (is (= (text buf {:liq.buffer/row 1 :liq.buffer/col 4} {:liq.buffer/row 2 :liq.buffer/col 1}) "\n"))
      (is (= (text buf {:liq.buffer/row 1 :liq.buffer/col 4} {:liq.buffer/row 9 :liq.buffer/col 1}) "\n\ndef"))
      (is (= (text buf {:liq.buffer/row 4 :liq.buffer/col 10} {:liq.buffer/row 4 :liq.buffer/col 20}) ""))
      (is (= (text buf {:liq.buffer/row 5 :liq.buffer/col 1} {:liq.buffer/row 5 :liq.buffer/col 2}) ""))
      (is (= (text buf {:liq.buffer/row 1 :liq.buffer/col 10} {:liq.buffer/row 1 :liq.buffer/col 1}) "abc")))))

(deftest word-test
  (testing "Get current word"
    (is (= (word (-> (buffer "aaa bbb ccc") (right 4))) "bbb"))
    (is (= (word (buffer "")) ""))))
  

(deftest delete-region-test
  (testing "Delete region"
    (is (= (-> (buffer "abc") (delete-region [{:liq.buffer/row 1 :liq.buffer/col 1} {:liq.buffer/row 1 :liq.buffer/col 3}]) text) ""))
    (is (= (-> (buffer "abc") (delete-region [{:liq.buffer/row 1 :liq.buffer/col 1} {:liq.buffer/row 1 :liq.buffer/col 4}]) text) ""))
    (is (= (-> (buffer "abc") (delete-region [{:liq.buffer/row 1 :liq.buffer/col 4} {:liq.buffer/row 1 :liq.buffer/col 4}]) text) "abc"))
    (is (= (-> (buffer "abc") (delete-region [{:liq.buffer/row 1 :liq.buffer/col 1} {:liq.buffer/row 1 :liq.buffer/col 5}]) text) ""))
    (is (= (-> (buffer "abc") (delete-region [{:liq.buffer/row 1 :liq.buffer/col 1} {:liq.buffer/row 1 :liq.buffer/col 1}]) text) "bc"))
    (is (= (-> (buffer "") (delete-region [{:liq.buffer/row 1 :liq.buffer/col 1} {:liq.buffer/row 1 :liq.buffer/col 1}]) text) ""))
    (is (= (-> (buffer "a\n") (delete-region [{:liq.buffer/row 1 :liq.buffer/col 2} {:liq.buffer/row 2 :liq.buffer/col 0}]) text) "a"))
    (is (= (-> (buffer "\n") (delete-region [{:liq.buffer/row 1 :liq.buffer/col 0} {:liq.buffer/row 2 :liq.buffer/col 0}]) text) ""))
    (is (= (-> (buffer "a\n") (delete-region [{:liq.buffer/row 1 :liq.buffer/col 2} {:liq.buffer/row 2 :liq.buffer/col 1}]) text) "a"))
    (is (= (-> (buffer "aa\n") (delete-region [{:liq.buffer/row 1 :liq.buffer/col 1} {:liq.buffer/row 2 :liq.buffer/col 0}]) text) ""))
    (is (= (-> (buffer "abc") (delete-region [{:liq.buffer/row 1 :liq.buffer/col 1} {:liq.buffer/row 1 :liq.buffer/col 3}]) text) ""))))

(deftest split-buffer-test
  (testing "Split buffer"
    (is (= (map text (split-buffer (buffer ""))) (list "" "")))
    (is (= (map text (split-buffer (buffer "aabb") {:liq.buffer/row 1 :liq.buffer/col 1})) (list "" "aabb")))
    (is (= (map text (split-buffer (buffer "aabb") {:liq.buffer/row 1 :liq.buffer/col 2})) (list "a" "abb")))
    (is (= (map text (split-buffer (buffer "aabb") {:liq.buffer/row 1 :liq.buffer/col 3})) (list "aa" "bb")))
    (is (= (map text (split-buffer (buffer "aabb") {:liq.buffer/row 1 :liq.buffer/col 4})) (list "aab" "b")))
    (is (= (map text (split-buffer (buffer "aabb") {:liq.buffer/row 1 :liq.buffer/col 5})) (list "aabb" "")))
    (is (= (map text (split-buffer (buffer "aa\nbb") {:liq.buffer/row 2 :liq.buffer/col 1})) (list "aa\n" "bb")))
    (is (= (map text (split-buffer (buffer "aa\n") {:liq.buffer/row 2 :liq.buffer/col 1})) (list "aa\n" "")))
    (is (= (map text (split-buffer (buffer "aa\n") {:liq.buffer/row 2 :liq.buffer/col 0})) (list "aa\n" "")))
    (is (= (map text (split-buffer (buffer "\nbb") {:liq.buffer/row 1 :liq.buffer/col 1})) (list "" "\nbb")))
    (is (= (map text (split-buffer (buffer "aabb") {:liq.buffer/row 1 :liq.buffer/col 3})) (list "aa" "bb")))))

(deftest end-of-word-test
  (testing "End-of-word"
    (is (= (-> (buffer "aaa bbb ccc") (right 100) end-of-word :liq.buffer/cursor) {:liq.buffer/row 1 :liq.buffer/col 11}))
    (is (= (-> (buffer "\n") down end-of-word :liq.buffer/cursor) {:liq.buffer/row 2 :liq.buffer/col 1}))))

(defn random-string
  [len]
  (let [chars ["a" "b" "c" "d" "e" "f" "g" "h" "i" "j" "k" "l" "m" "æ" "A" "B" "-" " " "\n" "\r" "$" "\t" "-" ":"]]
    (apply str (repeatedly len (fn [] (rand-nth chars))))
  ))

(defn random-buffer
  []
  (let [n (rand-int 20)]
    (cond (= n 0) (buffer "")
          (= n 1) (buffer "\n")
          (= n 2) (buffer "\n\n")
          (= n 3) (buffer "a")
          (= n 4) (buffer "\na")
          (= n 5) (buffer "a\n")
          true (buffer (random-string (rand-int 50000))))))

(defn random-textoperation
  [buf]
  (let [r (rand-int 18)]
    (cond (= r 0) (right buf 1)
          (= r 1) (right buf (rand-int 20))
          (= r 2) (left buf 1)
          (= r 3) (left buf (rand-int 20))
          (= r 4) (delete-char buf 1)
          (= r 5) (delete-char buf (rand-int 3))
          (= r 6) (end-of-line buf)
          (= r 7) (end-of-word buf)
          (= r 8) (beginning-of-buffer buf)
          (= r 9) (set-visual-mode buf)
          (= r 10) (set-normal-mode buf)
          (= r 11) (set-insert-mode buf)
          (= r 12) (delete buf)
          :else (insert-string buf (random-string (rand-int 200))))))

(defn generate
  [n]
  (nth (iterate random-textoperation (buffer "")) n))

(deftest properties-test
  (doseq [n (range 50)]
    (let [buf (generate (rand-int 200))]
      ;(testing "Point = count before"
      ;  (is (= (get-point buf) (-> buf before text count))))
      ;(testing "Linenumber = Total lines in before"
      ;  (is (= (get-linenumber buf) (total-lines (before sl)))))
      ;(testing "Totallines = total lines before and total lines after - 1"
      ;  (is (= (total-lines buf)
      ;         (+ (total-lines (before buf))
      ;            (total-lines (after buf))
      ;            -1))))
      (testing "Insert string -> delete (count string) is invariant"
        (let [len (rand-int 20)
              text (random-string len)]
          (is (= (buf (-> buf (insert-string text) (delete-char len)))))))
    )))

(deftest algebraic-properties-test
  (doseq [n (range 100)]
    (let [b1 (random-buffer)
          b2 (random-buffer)]
      (is (= (text (append-buffer b1 b2)) (str (text b1) (text b2))))
      (is (= (text (append-buffer b1 (buffer ""))) (text b1)))
      )))