(ns dk.salza.liq.slider-test
  (:require [clojure.test :refer :all]
            [dk.salza.liq.slider :refer :all]
            [dk.salza.liq.helper :refer :all]))

(defn sample ; "abcdef hij|1234hidden 78"
  []         ;    1      2 3          5
  (-> (create "abcdef hij1234hidden 78")
      (right 2)
      (set-mark "1")
      (right 7)
      (set-mark "2")
      (right 1)
      (set-mark "3")
      (right 10)
      (delete 6)
      (right 3)
      (set-mark "5")
      (left 7)
  ))

(defn random-string
  [len]
  (let [chars ["a" "b" "c" "d" "e" "f" "g" "h" "i" "j" "k" "l" "m" "æ" "A" "B" "-" " " "\n"]]
    (apply str (repeatedly len (fn [] (rand-nth chars))))
  ))

(defn random-textoperation
  [sl]
  (let [r (rand-int 11)]
    (cond (= r 0) (right sl 1)
          (= r 1) (right sl (rand-int 20))
          (= r 2) (left sl 1)
          (= r 3) (left sl (rand-int 20))
          (= r 4) (delete sl 1)
          (= r 5) (delete sl (rand-int 3))
          (= r 6) (end-of-line sl)
          (= r 7) (beginning sl)
          (= r 8) (set-meta sl :something "abc")
          :else (insert sl (random-string (rand-int 100))))))

(defn generate
  [n]
  (nth (iterate random-textoperation (create "")) n))

(deftest meta-test
  (testing "Setting and getting meta data"
    (let [sl (-> (create "abc\n123") right (set-meta :a "aa") (right 2) (set-meta :b "bb") (left 2))]
      (is (= (get-char sl) "b"))
      (is (= (get-meta sl :a) "aa"))
      (is (= (get-meta sl :non) nil))
      (is (= (-> sl right get-char) "c"))
      (is (= (-> sl right (get-meta :a)) nil))
      (is (= (-> sl (right 2) get-char) "\n"))
      (is (= (-> sl (right 2) (get-meta :b)) "bb"))
    )))

(deftest beginning-test
  (testing "Beginning of buffer"
    (is (= (-> (create "abc\n123") (right 10) (beginning) normalize-slider) "|abcn123"))))
  

(deftest get-visible-content-test
  (testing "Visible content"
    (is (= (-> (create "abc")
               (right 2)
               get-visible-content)
           "abc"))))

(deftest create-test
  (testing "Creating slider with text or as list"
    (is (= (create "abc\ndef") (create '("a" "b" "c" "\n" "d" "e" "f"))))
  ))

(deftest right-test
  (testing "Testing right"
    ;(is (= (-> sample (right 1) get-char) "2"))
    ;(is (= (get-char (right (sample) 1)) "2"))
    ))

(deftest right-1-performance
  (let [sl0 (create (clojure.string/join "\n" (repeat 2000 "aaaaaaa aaaaaaaa aaaaa aaaaaa aaaaaa")))]
    ;; Implementation old takes 150 ms 
    (testing "Performance when doing (right sl 1)"
      (time
        (loop [sl sl0]
          (if (> (get-linenumber sl) 1500)
            sl
            (recur (right sl 1))))
      )
      
      ;(is (= "Not implemented yet" ""))
  )))

(deftest set-point-test
  (testing "Set point"
    (is (= (-> (create "aaa") (set-point 0) get-point) 0))
    (is (= (-> (create "aaa") (right 1) (set-point 0) get-point) 0))
    (is (= (-> (create "aaaa bbbb cccc") (right 6) (set-point 4) get-point) 4))
  ))

(deftest insert-test
  (testing "Inserting a string"
    (is (= (-> (sample)
               (insert "xyz")
               get-visible-content)
           "abcdef hijxyz1234 78")))
  (testing "Multiple inserts"
    (is (= (-> (create "abc")
               (insert "xyz")
               (insert "123")
               get-visible-content)
           "xyz123abc")))
  (testing "Position when inserting a string"
    (is (= (-> (create "abc")
               (insert "xyz")
               (insert "123")
               get-point)
           6)))
  (testing "Linenumber when inserting a string with newlines"
    (let [sl (-> (create "abc\n\ndef") (insert "xyz\n\n\n"))]
      (is (= (-> sl get-linenumber) 4))
      (is (= (-> sl (right 3) get-linenumber) 4))
      (is (= (-> sl (right 4) get-linenumber) 5))
      (is (= (-> sl (right 5) get-linenumber) 6))
      (is (= (-> sl (right 6) get-linenumber) 6))
    ))
  )

(deftest slider-delete-test
  (testing "Deleting a string"
    (let [sl (-> (create "aaabbcde")
                 (right 6))]
      (is (= (-> sl (delete 3) (get-content)) "aaade"))
      (is (= (-> sl (delete 20) (get-content)) "de"))
    )))

(deftest slider-wrap-test
  (testing "Wrapping text"
    (let [sl (create "a\nab\nabc\nabcd\nabcde\na a\naa a\naaa a\naaaa a\n\n\nabc\ndddddddddd")]
      (is (= (-> sl (wrap 3) (get-content)) 
          "a\nab\nabc\nabc\nd\nabc\nde\na a\naa \na\naaa\n a\naaa\na a\n\n\nabc\nddd\nddd\nddd\nd")))))

(deftest slider-pad-right-test
  (testing "Padding to the right"
    (let [sl (create "a\nab\nabc\nabcd\nabcde\na a\naa a\naaa a\naaaa a\n\n\nabc\ndddddddddd")]
      (is (= (-> sl (pad-right 4) (get-content)) 
          "a   \nab  \nabc \nabcd\nabcde\na a \naa a\naaa a\naaaa a\n    \n    \nabc \ndddddddddd")))))


(deftest forward-word-test
  (let [sl0 (create "abc def\nhij")
        sl1 (forward-word sl0)
        sl2 (forward-word sl1)
        sl3 (forward-word sl2)]
    (testing "Forward word basic cases"
      (is (= (get-char sl0) "a"))
      (is (= (get-char sl1) "d"))
      (is (= (get-char sl2) "h"))
      (is (= (get-char sl3) nil))))
  (testing "Ending with newline"
    (is (= (-> (create "abc\n") forward-word get-point) 4)))
  (testing "Ending with space"
    (is (= (-> (create "abc ") forward-word get-point) 4))))

(deftest forward-line-test
  ;;                 abcdef|ghi |jkl|mn |opqrst||vx|
  (let [sl0 (create "abcdefghi jkl\nmn opqrst\n\nvx")
        sl1 (forward-line sl0 6)
        sl2 (forward-line sl1 6)
        sl3 (forward-line sl2 6)
        sl4 (forward-line sl3 6)
        sl5 (forward-line sl4 6)
        sl6 (forward-line sl5 6)
        sl7 (forward-line sl6 6)]
    (testing "Forward line with columns"
      (is (= (get-char sl0) "a"))
      (is (= (get-char sl1) "g"))
      (is (= (get-char sl2) "j"))
      (is (= (get-char sl3) "m"))
      (is (= (get-char sl4) "o"))
      (is (= (get-char sl5) "\n"))
      (is (= (get-char sl6) "v"))
      (is (= (get-char sl7) nil))))
  (testing "Forward line without columns"
    (is (= (-> (create "abcd\n hij") forward-line get-char) " "))
    (is (= (-> (create "abcd hij") forward-line end?) true)))
  (testing "Forward line edge cases"
    (is (= (-> (create "abcd\nhij") (forward-line 4) get-char) "h"))
    (is (= (-> (create "abc\nhij") (forward-line 4) get-char) "h"))
    (is (= (-> (create "abcd\nhij") (forward-line 3) get-char) "d"))
    (is (= (-> (create " abcd\nhij") (forward-line 4) get-char) "a"))
    (is (= (-> (create " abcd hij") (forward-line 20) get-char) nil))))

(deftest get-visual-column-test
  (let [sl (create "abc def\nhij")]
    (testing "Visual column calculations."
      (is (= (get-visual-column (right sl 5) 4) 1))
      (is (= (get-visual-column (right sl 1) 4) 1))
      (is (= (get-visual-column (right sl 8) 4) 0))
      (is (= (get-visual-column (right sl 20) 4) 3))
    )))

(deftest forward-visual-column-test
  (let [sl (create "abc def\nhij")]
    (testing "Forward visual column calculations."
      (is (= (-> sl (right 1) (forward-visual-column 4 1) get-char) "e"))
    )))

(deftest get-region-test
  ;;                    | | |      |    C       |
  ;;                    01234 5678 9012345678901
  (let [sl (-> (create "abcd\nefg\nhijklmnopqrst")
               (set-mark "m0")
               (right 2)
               (set-mark "m2")
               (right 2)
               (set-mark "m4")
               (right 5)
               (set-mark "m9")
               (right 100)
               (set-mark "mend")
               (left 8))]
    (testing "Get region cases"
      (is (= (get-region sl "m0") "abcd\nefg\nhijkl"))
      (is (= (get-region sl "notexist") nil))
      (is (= (get-region sl "m9") "hijkl"))
      (is (= (get-region sl "mend") "mnopqrst"))
    ))) 

(deftest look-behind-test
  ;;                    | | |      |    C       |
  ;;                    01234 5678 9012345678901
  (let [sl (-> (create "abcd\nefg\nhijklmnopqrst")
               (right 14))]
    (testing "Look behind"
      (is (= (look-behind sl 1) "l"))
      (is (= (look-behind sl 6) "\n"))
      (is (= (look-behind sl 9) "e"))
      (is (= (look-behind sl 10) "\n"))
      (is (= (look-behind sl 14) "a"))
      (is (= (look-behind sl 15) nil))
      (is (= (look-behind sl 25) nil))
    )))

(deftest look-ahead-test
  ;;                    | | |  C   |            |
  ;;                    01234 5678 9012345678901
  (let [sl (-> (create "abcd\nefg\nhijklmnopqrst")
               (right 6))]
    (testing "Look ahead"
      (is (= (look-ahead sl 0) "f"))
      (is (= (look-ahead sl 1) "g"))
      (is (= (look-ahead sl 2) "\n"))
      (is (= (look-ahead sl 3) "h"))
      (is (= (look-ahead sl 15) "t"))
      (is (= (look-ahead sl 16) nil))
      (is (= (look-ahead sl 26) nil))
    )))

(deftest string-ahead-test
  ;;                    | | |  C   |            |
  ;;                    01234 5678 9012345678901
  (let [sl (-> (create "abcd\nefg\nhijklmnopqrst")
               (right 6))]
    (testing "String ahead"
      (is (= (string-ahead sl 1) "f"))
      (is (= (string-ahead sl 2) "fg"))
      (is (= (string-ahead sl 3) "fg\n"))
      (is (= (string-ahead sl 4) "fg\nh"))
      (is (= (string-ahead sl 16) "fg\nhijklmnopqrst"))
      (is (= (string-ahead sl 17) "fg\nhijklmnopqrst"))
      (is (= (string-ahead sl 27) "fg\nhijklmnopqrst"))
    )))

(deftest take-lines-test
  (let [sl (create "aaa\n1\n22\n333\n4444\n55555\nbb bb bbb\ncccc cccc ccccc")]
    (testing "Takes lines cases"
      (is (= (take-lines sl 7 4) '("aaa" "1" "22" "333" "4444" "5555" "5")))
      (is (= (take-lines (right sl 22) 13 4) '("5" "bb " "bb " "bbb" "cccc" " " "cccc" " " "cccc" "c" "" "" "")))
      (is (= (take-lines (create "abc\ndef\n") 5 4) '("abc" "def" "" "" "")))
    )))

(deftest find-next-test
  (let [sl (-> (create "aaa\nbbcdef ghi\njklm") (right 7))]
    (testing "Basic search for next text."
      (is (= (find-next sl "aaa") sl))
      (is (= (find-next sl "def") sl))
      (is (= (find-next sl "ef g") (right sl 1)))
      (is (= (find-next sl "m") (right sl 11)))

   )))

(deftest before-test
  (let [sl1 (-> (create "a\ncd") (right 4) (set-mark "a"))
        sl2 (-> sl1 (insert "e") (set-mark "b") (left 1) (set-dirty false))]
    (testing "Before function removes chars and marks after cursor."
      (is (= sl1 (before sl2))))))

(deftest after-test
  (let [sl1 (-> (create "d\nf") (set-mark "a"))
        sl2 (-> (create "a\ncd\nf") (right 2) (set-mark "b") (right 1) (set-mark "a"))]
    (testing "After function removes chars and marks before cursor."
      (is (= sl1 (after sl2)))
      (is (= (after sl2) (after (after sl2))))
      (is (= (beginning sl1) (after (beginning sl1)))))))

(deftest insert-slider-test
  (let [sl1 (-> "a\nbcde\nfg" (create) (right 3))
        sl2 (-> "aaaabbb\ncccc\ndddd" (create) (right 9))
        expected (-> "a\nbaaaabbb\ncccc\nddddcde\nfg" (create) (right 12))]
    (testing "Inserting slider into another"
      (is (= (-> (insert-slider sl1 sl2) (set-dirty false)) expected)))))

;(deftest frame-test
;  (let [sl (create "aaa\n1\n22\n333\n4444\n55555\nbb bb bbb\ncccc cccc ccccc")]
;    (testing "Frame lines"
;      (is (= ((frame sl 7 4 0) :lines) '("aaa" "1" "22" "333" "4444" "5555" "5")))
;      (is (= ((frame (-> (create "aaa\nbbb\nccc\nddd") (right 20)) 4 4 0) :lines) '("aaa" "bbb" "ccc" "ddd")))
;      (is (= ((frame (-> (create "aaa\nbbb\nccc\nddd\n") (right 20)) 4 4 0) :lines) '("" "" "" "")))
;      (is (= ((frame (-> (create "aaa\nbbb\nccc\nddd\neee") (right 16)) 4 4 0) :lines) '("eee" "" "" "")))
;      (is (= ((frame (-> (create "aaa\nbbb\nccc\nddd\neee\n") (right 16)) 4 4 0) :lines) '("eee" "" "" ""))))
;    (testing "Frame cursor"
;      (is (= ((frame (-> (create "aaa\nbbb\nccc\nddd") (right 0)) 4 4 0) :cursor) {:row 1 :column 1}))
;      (is (= ((frame (-> (create "aaa\nbbb\nccc\nddd") (right 1)) 4 4 0) :cursor) {:row 1 :column 2}))
;      (is (= ((frame (-> (create "aaa\nbbb\nccc\nddd") (right 2)) 4 4 0) :cursor) {:row 1 :column 3}))
;      (is (= ((frame (-> (create "aaa\nbbb\nccc\nddd") (right 3)) 4 4 0) :cursor) {:row 1 :column 4}))
;      (is (= ((frame (-> (create "aaa\nbbb\nccc\nddd") (right 4)) 4 4 0) :cursor) {:row 2 :column 1}))
;      (is (= ((frame (-> (create "aaa\nbbb\nccc\nddd") (right 5)) 4 4 0) :cursor) {:row 2 :column 2}))
;      (is (= ((frame (-> (create "aaa\nbbb\nccc\nddd\neee") (right 20)) 4 4 0) :cursor) {:row 1 :column 4}))
;      (is (= ((frame (-> (create "aaa\nbbb\nccc\nddd\neee\n") (right 25)) 4 4 0) :cursor) {:row 1 :column 1}))
;    )))

(deftest point-to-mark-nil-test
  (testing "Moving point to nonexiting mark."
    (let [sl (-> (create "abcde") (right 2))]
      (is (= (-> sl (point-to-mark "nonexist") (get-point)) 2)))))

  
(deftest properties-test
  (doseq [n (range 20)]
    (let [sl (generate (rand-int 500))]
      (testing "Point = count before"
        (is (= (get-point sl) (count (sl :dk.salza.liq.slider/before)))))
      (testing "Linenumber = count linebreaks in before + 1"
        (is (= (get-linenumber sl) (+ (count (filter is-newline? (sl :dk.salza.liq.slider/before))) 1))))
      (testing "Totallines = count linebreaks in before and linebreakes after + 1"
        (is (= (sl :dk.salza.liq.slider/totallines)
               (+ (count (filter is-newline? (sl :dk.salza.liq.slider/before)))
                  (count (filter is-newline? (sl :dk.salza.liq.slider/after)))
                  1))))
      (testing "Insert string -> delete (count string) is invariant"
        (let [len (rand-int 100)
              text (random-string len)]
          (is (= (sl (-> sl (insert text) (delete len)))))))
    )))