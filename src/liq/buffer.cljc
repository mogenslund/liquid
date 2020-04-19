(ns liq.buffer
  (:require [clojure.string :as str]))

;; TODO Use points and regions whereever it makes sense
;; and depricate functions taking row and col as input when
;; actually a point is appropriate.

;; TODO Use regions and actions on regions, like:
;; end-of-word use word-region, change change-word
;; ord delete-word.
;; In addition: change-outer-word, etc.

(defn buffer
  "Create a buffer map.
  Input: Various fields
  Output: Buffer
  Side effects: None" 
  ([text {:keys [name filename top left rows cols major-mode major-modes mode] :as options}]
   (let [lines (mapv (fn [l] (mapv #(hash-map ::char %) l)) (str/split text #"\r?\n" -1))]
     {::name (or name "")
      ::filename filename
      ::lines lines
      ::lines-undo ()  ;; Conj lines into this when doing changes
      ::lines-stack (list {::lines lines ::cursor {::row 1 ::col 1}}) ;; To use in connection with undo
      ::line-ending "\n" 
      ::hidden-lines {}
      ::cursor {::row 1 ::col 1}
      ::selection nil
      ::window {::top (or top 1) ::left (or left 1) ::rows (or rows 1) ::cols (or cols 80)}
      ::mem-col 1                ; Remember column when moving up and down
      ::tow {::row 1 ::col 1}    ; Top of window
      ::mode (or mode :normal)
      ::encoding :utf-8          ; This allows cursor to be "after line", like vim. (Separate from major and minor modes!)
      ::search-word ""
      ::dirty false
      ::major-modes (or major-modes (list :spacemacs-mode :clojure-mode :fundamental-mode))}))
  ([text] (buffer text {})))

(defn insert-in-vector
  "Insert an element into a vector, at a given position.
  (insert-in-vector [:a :c :d] 1 :b) -> [:a :b :c :d])
  Input: Vector, position, element
  Output: Vector
  Side effects: None"
  [v n elem]
  (into [] (concat
             (into [] (subvec v 0 n))
             [elem]
             (into [] (subvec v n)))))

(defn remove-from-vector
  ([v n]
   (if (<= 1 n (count v))
     (into [] (concat
                (into [] (subvec v 0 (dec n)))
                (into [] (subvec v n))))
     v))
  ([v m n]
    (if (<= 1 m n (count v))
     (into [] (concat
                (into [] (subvec v 0 (dec m)))
                (into [] (subvec v n))))
     v)))

(defn sub-buffer
  "Buffer with row1 as first row and row2 as last.
  Cursor translated accordingly." 
  [buf row1 row2]
  (-> buf
      (update ::lines subvec (dec row1) row2)
      (update-in [::cursor ::row] #(max (- % row1 -1) 1))))

(defn set-undo-point
  "Return new lines with the current lines to the undo stack.
  Input: Buffer
  Output: Buffer
  Side effects: None"
  [buf]
  (let [newstack (conj (buf ::lines-stack) (select-keys buf [::lines ::cursor]))]
    (assoc buf ::lines-stack newstack
               ::lines-undo newstack)))

(defn undo
  "Returns the first buffer in the undo stack."
  [buf]
  (if (empty? (buf ::lines-undo))
    buf
    (assoc buf ::lines (-> buf ::lines-undo first ::lines)
               ::cursor (-> buf ::lines-undo first ::cursor)
               ::lines-stack (conj (buf ::lines-stack) (-> buf ::lines-undo first))
               ::lines-undo (rest (buf ::lines-undo)))))

(defn debug-clear-undo
  [buf]
  (assoc buf
    ::lines-undo (list)
    ::lines-stack (list)))

;; Information
;; ===========

(defn line-count
  "Number of lines in the buffer."
  [buf]
  (count (buf ::lines)))

(defn col-count
  "Number of columns on a given line in a buffer."
  [buf row]
  (-> buf ::lines (get (dec row)) count))

(comment 
  (let [buf (buffer "abcd\nxyz")]
    (-> buf
        (set-mode :insert)
        get-mode)))

(defn update-mem-col
  [buf]
  (assoc buf ::mem-col ((buf ::cursor) ::col)))

(defn point-compare
  "Compares two point p1 and p2.
  If p1 is before p2 -1 is returned.
  If p1 is after p2 1 is returned.
  If p1=p2 0 is returned."
  [p1 p2]
  (compare [(p1 ::row) (p1 ::col)]
           [(p2 ::row) (p2 ::col)]))

(defn set-selection
  "Set the selection point. If no point is given,
  the current cursor position will be used."
  ([buf p] (assoc buf ::selection p))
  ([buf row col] (set-selection buf {::row row ::col col}))
  ([buf] (set-selection buf (buf ::cursor))))

(defn get-selection
  "Return the selection point.
  One can also use
  (buf ::buffer/selection)
  directly."
  [buf]
  (buf ::selection))

(defn remove-selection
  "Set selection point to nil."
  [buf]
  (assoc buf ::selection nil))

(defn set-visual-mode
  "Set mode to :visual and set selection point."
  [buf]
  (-> buf
      (assoc ::mode :visual)
      set-selection))

(defn set-normal-mode
  "Set mode to :normal and set selection point to nil."
  [buf]
  (-> buf
      (assoc ::mode :normal)
      remove-selection))
  
(defn set-insert-mode
  "Set mode to :insert and set selection point to nil."
  [buf]
  (-> buf
      set-undo-point
      (assoc ::mode :insert)
      remove-selection))

(defn expand-selection
  "Expand a selection with a region"
  [buf r]
  (update-mem-col
    (cond (nil? (buf ::selection)) (assoc (set-visual-mode buf) ::selection (first r) ::cursor (second r))
          (= (buf ::selection) (buf ::cursor)) (assoc buf ::selection (first r) ::cursor (second r))
          (< (point-compare (buf ::cursor) (buf ::selection)) 0) (assoc buf ::cursor (first r))
          (> (point-compare (buf ::cursor) (buf ::selection)) 0) (assoc buf ::cursor (second r))
          true buf)))

(defn set-dirty
  "Set the buffer as dirty.
  Should be called when a change has been made which is
  not saved yet."
  [buf val]
  (if (buf ::filename)
    (assoc buf ::dirty val)
    buf))

(defn dirty?
  "Return true if the buffer is changed since last save."
  [buf]
  (buf ::dirty))

(defn adjust-hidden-rows
  [buf row n]
  (update buf ::hidden-lines 
    #(into {} (for [[k v] %] (if (>= k row) [(+ k n) (+ v n)] [k v])))))

(defn row-hidden?
  [buf row]
  (some true? (for [[k v] (buf ::hidden-lines)] (<= k row v))))

(defn visible-rows-count
  [buf row1 row2]
  (if (< row2 row1)
    0
    (reduce #(+ %1 (if (row-hidden? buf %2) 0 1)) 0 (range row1 (inc row2)))))

(comment (row-hidden? (-> (buffer "aaa\nbbb\nccc\nddd") (assoc ::hidden-lines {2 3})) 1))
(comment (row-hidden? (-> (buffer "aaa\nbbb\nccc\nddd")) 1))
(comment (row-hidden? (-> (buffer "aaa\nbbb\nccc\nddd") (assoc ::hidden-lines {2 3})) 2))
(comment (visible-rows-count (-> (buffer "aaa\nbbb\nccc\nddd") (assoc ::hidden-lines {2 3})) 1 4))
(comment (visible-rows-count (-> (buffer "aaa\nbbb\nccc\nddd") (assoc ::hidden-lines {2 3})) 3 3))
(comment (visible-rows-count (-> (buffer "aaa\nbbb\nccc\nddd") (assoc ::hidden-lines {2 3})) 4 4))
(comment (visible-rows-count (-> (buffer "aaa\nbbb\nccc\nddd")) 1 4))

(defn next-visible-row
  ([buf row]
   (reduce #(if (<= (first %2) %1 (second %2))
              (inc (second %2))
              %1)
          (inc row) (buf ::hidden-lines)))
  ([buf]
   (next-visible-row buf (-> buf ::cursor ::row))))

(comment (next-visible-row (buffer "aaa\nbbb\nccc\ndddd\neee")))
(comment (nth (iterate #(next-visible-row (buffer "aaa\nbbb\nccc\ndddd\neee") %) 1) 3))

(defn previous-visible-row
  ([buf row]
   (reduce #(if (<= (first %2) %1 (second %2))
              (dec (first %2))
              %1)
          (dec row) (buf ::hidden-lines)))
  ([buf]
   (previous-visible-row buf (-> buf ::cursor ::row))))


(defn line
  "Get line as string"
  ([buf row]
   (str/join (map ::char (get (buf ::lines) (dec row)))))
  ([buf row col]
   (let [r (get (buf ::lines) (dec row))]
     (if (> col (count r))
       ""
       (str/join (map ::char (subvec r (dec col)))))))
  ([buf] (line buf (-> buf ::cursor ::row))))

(comment (pr-str (line (buffer "aaa\nbbb\nccc") 2)))

(comment
  (str/join (map ::char (get ((buffer "abcde") ::lines) 1)))
  (line (buffer "abcde") 1)
  (line (buffer "abcde") 1 2)
  (type (line (buffer "abcde") 2 2))
  (type (line (buffer "abcde") 10))
  (line (buffer "abcde") 1 10))

(defn word
  "Get word at a given point point."
  ([buf row col]
   (loop [l (str/split (line buf row) #" ") idx 1]
     (let [w (first l)]
       (if (or (> (+ (count w) idx) col) (empty? l))
       w
       (recur (rest l) (+ idx (count w) 1))))))
  ([buf p] (word buf (p ::row) (p ::col)))
  ([buf] (word buf (-> buf ::cursor ::row) (-> buf ::cursor ::col))))

(defn text
  "Get the text ind the buffer.
  If a region or two points is specified the text will be the
  contained text."
  ([buf]
   (str/join "\n" (map (fn [line] (str/join "" (map ::char line))) (buf ::lines))))
  ([buf p1 p2]
    (let [p (if (= (point-compare p1 p2) -1) p1 p2)  ; first 
          q (if (= (point-compare p1 p2) -1) p2 p1)  ; second
          lines (buf ::lines)]
    (str/join "\n"
      (filter #(not (nil? %))
        (for [n (range (count lines))]
          (cond (< (inc n) (p ::row)) nil
                (= (inc n) (p ::row) (q ::row)) (str/join "" (map ::char (subvec (lines n) (dec (p ::col)) (min (q ::col) (count (lines n))))))
                (= (inc n) (p ::row)) (str/join "" (map ::char (subvec (lines n) (dec (p ::col)))))
                (= (inc n) (q ::row)) (str/join "" (map ::char (subvec (lines n) 0 (min (q ::col) (count (lines n))))))
                (>= n (q ::row)) nil
                true (str/join "" (map ::char (lines n)))))))))
  ([buf r] (text buf (first r) (second r))))

(comment
  (text (buffer "abcdefg\n1234567\nABCDEF" {}) {::row 1 ::col 1} {::row 2 ::col 3})
  (text (buffer "abcdefg\n1234567\nABCDEF" {}) {::row 1 ::col 2} {::row 2 ::col 3})
  (text (buffer "abcdefg\n1234567\nABCDEF" {}) {::row 2 ::col 2} {::row 2 ::col 3})
  (text (buffer "abcdefg\n\nABCDEF\n\n" {}) {::row 1 ::col 2} {::row 6 ::col 1} )
)

(defn previous-point
  "If only a buffer is supplied the point will be set
  on the buffer, otherwise:
  The previous point will be returned or nil, if the
  input is the first point"  
  ([buf p]
   (cond (> (p ::col) 1) (update-in p [::col] dec)
         (> (p ::row) 1) {::row (dec (p ::row)) ::col (col-count buf (dec (p ::row)))})) 
  ([buf] (if (= (buf ::cursor) {::row 1 ::col 1})
           buf
           (assoc buf ::cursor (previous-point buf (buf ::cursor))))))

(defn next-point
  ([buf p]
   (cond (< (p ::col) (col-count buf (p ::row))) (update-in p [::col] inc)
         (< (p ::row) (line-count buf)) {::row (inc (p ::row)) ::col (min 1 (col-count buf (inc (p ::row))))})) 
  ([buf] (if-let [p (next-point buf (buf ::cursor))]
           (assoc buf ::cursor p)
           buf)))

(comment (next-point (buffer "aaa\n\nbbb\nccc") {::row 5 ::col 1}))
(comment (previous-point (buffer "aaa\n\nbbb\nccc") {::row 2 ::col 1}))
(comment (previous-point (buffer "aaa\n\nbbb\nccc") {::row 1 ::col 1}))
(comment
  (let [buf (buffer "aaa\n\nbbb\nccc")]
    (loop [p {::row 4 ::col 3}]
      (when (previous-point buf p)
        (println (previous-point buf p))
        (recur (previous-point buf p))))))

(defn end-point
  "The last point in the buffer"
  [buf]
  {::row (line-count buf) ::col (col-count buf (line-count buf))})

(comment
  (end-point (buffer "aaaa bbbb\nccc")))

(defn start-point
  [buf]
  {::row 1 ::col 1})

(defn eol-point
  ([buf p]
   (assoc p ::col (col-count buf (p ::row))))
  ([buf]
   (eol-point buf (-> buf ::cursor))))

(comment (eol-point (buffer "aaaa bbbb\nccc")))
(comment (eol-point (buffer "aaaa bbbb\nccc") {::row 2 ::col 1}))
 

(defn get-selected-text
  [buf]
  (if-let [p (get-selection buf)]
    (text buf (buf ::cursor) p)
    ""))

(defn selected?
  ([buf p]
   (let [s (get-selection buf)
         c (buf ::cursor)]
     (cond (nil? s) false
           (and (<= (point-compare s p) 0) (<= (point-compare p c) 0)) true
           (and (<= (point-compare c p) 0) (<= (point-compare p s) 0)) true
           true false)))
   ([buf row col] (selected? buf {::row row ::col col})))

;; Movements
;; =========

(defn right
  "Move cursor forward.
  If n is specifed forward n steps."
  ([buf n]
   (let [linevec (-> buf ::lines (get (dec (-> buf ::cursor ::row))))
         maxcol (+ (count linevec) (if (= (buf ::mode) :insert) 1 0))
         newcol (max 1 (min maxcol (+ (-> buf ::cursor ::col) n)))]
     (-> buf
         (assoc ::cursor {::row (-> buf ::cursor ::row) ::col newcol})
         (assoc ::mem-col newcol)))) 
  ([buf]
   (right buf 1)))

(defn left
  "Move cursor backward.
  If n is specifed backward n steps."
  ([buf n]
   (right buf (- n)))
  ([buf]
   (left buf 1)))

(defn down
  "Move cursor down.
  If n is specified move down n steps."
  ([buf n]
   (let [;newrow (max 1 (min (count (buf ::lines)) (+ (-> buf ::cursor ::row) n hide-inc)))
         newrow (max 1 (min (count (buf ::lines)) (+ (next-visible-row buf) (dec n))))
         linevec (-> buf ::lines (get (dec newrow)))
         maxcol (+ (count linevec) (if (= (buf ::mode) :insert) 1 0))
         newcol (max 1 (min maxcol (buf ::mem-col)))]
     (assoc buf ::cursor {::row newrow ::col newcol}))) 
  ([buf]
   (down buf 1)))

(defn up
  "Move cursor up.
  If n is specified move up n steps."
  ([buf n]
   (let [newrow (max 1 (min (count (buf ::lines)) (- (previous-visible-row buf) (dec n))))
         linevec (-> buf ::lines (get (dec newrow)))
         maxcol (+ (count linevec) (if (= (buf ::mode) :insert) 1 0))
         newcol (max 1 (min maxcol (buf ::mem-col)))]
     (assoc buf ::cursor {::row newrow ::col newcol})))
  ([buf]
   (up buf 1)))

(defn end-of-line
  "Move cursor to end of current line"
  [buf]
  (-> buf
      (assoc ::cursor {::row (-> buf ::cursor ::row) ::col (col-count buf (-> buf ::cursor ::row))}) 
      (assoc ::mem-col (col-count buf (-> buf ::cursor ::row)))))

(defn beginning-of-line
  "Move cursor to beginning of current line"
  [buf]
  (-> buf
      (assoc ::cursor {::row (-> buf ::cursor ::row) ::col 1}) 
      (assoc ::mem-col 1)))

(defn beginning-of-buffer
  "Move cursor to the beginning of the buffer"
  [buf]
  (-> buf
      (assoc ::cursor {::row 1 ::col 1}) 
      (assoc ::mem-col 1)))


(defn end-of-buffer
  "Move cursor to the end of the buffer"
  [buf]
  (-> buf
      (assoc ::cursor {::row (line-count buf) ::col (col-count buf (line-count buf))})
      (assoc ::mem-col (col-count buf (line-count buf)))))

;; Modifications
;; =============


(defn append-line-at-end
  "Append empty lines at end"
  ([buf n]
   (loop [buf0 buf n0 n]
     (if (<= n0 0)
       (set-dirty buf0 true)
       (recur (update buf0 ::lines conj []) (dec n0)))))
  ([buf] (append-line-at-end buf 1)))

(defn append-spaces-to-row
  [buf row n]
  (update-in (set-dirty buf true) [::lines (dec row)] #(into [] (concat % (repeat n {::char \space}))))) 

(comment
  (let [buf (buffer "abcd\nxyz")
        row 4
        spaces 5]
    (append-spaces-to-row buf 2 10)
  ))

(defn get-char
  "Get the character symbol at a given position."
  ([buf row col]
   (-> buf
       ::lines
       (get (dec row))
       (get (dec col))
       ::char))
  ([buf p]
   (get-char buf (p ::row) (p ::col)))
  ([buf]
   (get-char buf (-> buf ::cursor ::row) (-> buf ::cursor ::col))))


(comment
  
  (get-char (buffer "abcd\nxyz"))
  (get-char (buffer "abcd\nxyz") {::row 1 ::col 1})

  (let [buf (buffer "abcd\nxyz")]
    (-> buf
        (get-char 2 3))

  (let [buf (buffer "abcd\n\nxyz")]
    (-> buf
        down
        get-char))))


(defn get-attribute
  [buf attr]
  (-> buf
      ::lines
      (get (dec (-> buf ::cursor ::row)))
      (get (dec (-> buf ::cursor ::col))) attr))

;(into [] (subvec v 0 n))
(defn insert-line-break
  [buf row col]
  (-> buf
      (set-dirty true)
      (update ::lines
        (fn [lines]
          (let [l (lines (dec row))
                l1 (into [] (subvec l 0 (dec col)))
                l2 (into [] (subvec l (dec col)))]
            (-> lines
                (assoc (dec row) l1)
                (insert-in-vector row l2)))))
      (adjust-hidden-rows (inc row) 1)))

(defn set-char
  ([buf row col char]
   (-> buf
       (set-dirty true)
       (append-line-at-end (- row (line-count buf)))
       (append-spaces-to-row row (- col (col-count buf row)))
       (assoc-in [::lines (dec row) (dec col)] {::char char})))
  ([buf p char] (set-char buf (p ::row) (p ::col) char))
  ([buf char] (set-char buf (-> buf ::cursor) char)))

(defn get-style
  ([buf row col]
   (-> buf
       ::lines
       (get (dec row))
       (get (dec col))
       ::style))
  ([buf] (get-style buf (-> buf ::cursor ::row) (-> buf ::cursor ::col))))

(defn set-style
  ([buf row col style]
   (if (and (get-char buf row col) (not= (get-style buf row col) style))
     (assoc-in buf [::lines (dec row) (dec col) ::style] style)
     buf))
  ([buf p style] (set-style buf (p ::row) (p ::col) style))
  ([buf style] (set-style buf (-> buf ::cursor ::row) (-> buf ::cursor ::col) style))
  ([buf row col1 col2 style]
   (loop [b buf col col1]
     (if (> col col2)
       b
       (recur (set-style b row col style) (inc col))))))

(defn insert-char
  ([buf row col char]
   (if (= char \newline)
     (insert-line-break buf row col)
     (update-in (set-dirty buf true) [::lines (dec row)] #(insert-in-vector % (dec col) {::char char}))))
  ([buf char]
   (-> buf
       (insert-char (-> buf ::cursor ::row) (-> buf ::cursor ::col) char)
       (assoc ::cursor {::row (if (= char \newline) (inc (-> buf ::cursor ::row)) (-> buf ::cursor ::row))
                   ::col (if (= char \newline) 1 (inc (-> buf ::cursor ::col)))}))))


(defn append-line
  ([buf row]
   (-> buf
       set-insert-mode
       (set-dirty true)
       (update ::lines #(insert-in-vector % row []))
       (assoc ::cursor {::row (inc (-> buf ::cursor ::row)) ::col 1})
       (adjust-hidden-rows (inc row) 1)))
  ([buf]
   (append-line buf (-> buf ::cursor ::row))))

(defn delete-char
  ([buf row col n]
   (update-in (set-undo-point (set-dirty buf true)) [::lines (dec row)] #(remove-from-vector % col (+ col n -1))))
  ([buf n]
   (-> buf
       (delete-char (-> buf ::cursor ::row) (-> buf ::cursor ::col) n)))
  ([buf]
   (-> buf
       (delete-char (-> buf ::cursor ::row) (-> buf ::cursor ::col) 1))))

(defn delete-line
  ([buf row]
   (if (<= (line-count buf) 1)
     (assoc (set-undo-point buf) ::lines [[]]
             ::cursor {::row 1 ::col 1}
             ::mem-col 1)
     (let [b1 (update buf ::lines #(remove-from-vector % row))
           newrow (min (line-count b1) row)
           newcol (min (col-count b1 newrow) (-> buf ::cursor ::col))]
        (-> b1
            (assoc ::cursor {::row newrow ::col newcol})
            (adjust-hidden-rows row -1)))))
  ([buf] (delete-line buf (-> buf ::cursor ::row))))

(comment (pr-str (text (-> (buffer "aaa\nbbb\nccc") down right delete-line))))
(comment (pr-str (text (-> (buffer "aaa\nbbb\nccc") down down delete-line))))

(defn delete
  ([buf p1 p2]
    (let [p (if (= (point-compare p1 p2) -1) p1 p2)  ; first 
          q (if (= (point-compare p1 p2) -1) p2 p1)  ; second
          t1 (if (> (p ::col) 1)
               (subvec (-> buf ::lines (get (dec (p ::row)))) 0 (dec (p ::col)))
               [])  
          t2 (if (< (q ::col) (col-count buf (q ::row)))
               (subvec (-> buf ::lines (get (dec (q ::row)))) (q ::col) (col-count buf (q ::row)))
               [])
          buf1 (set-undo-point buf)]  
      (-> (nth (iterate #(delete-line % (p ::row))
                        (update buf1 ::lines
                                    #(insert-in-vector % (q ::row) (into [] (concat t1 t2)))))
               (- (q ::row) (p ::row) -1))
          set-normal-mode
          (assoc ::cursor p)
          (adjust-hidden-rows (p ::row) 1)
          (set-dirty true))))
  ([buf]
   (if-let [p (get-selection buf)]
     (delete buf (buf ::cursor) p)
     buf)))


(comment (pr-str (text (delete (buffer "aa\naa\naa") {::row 1 ::col 2} {::row 2 ::col 2}))))
(comment (pr-str (text (delete (buffer "aa\nbb\ncc") {::row 2 ::col 2} {::row 3 ::col 2}))))
(comment (pr-str (text (delete (buffer "aa\nbbccdd\nee") {::row 2 ::col 3} {::row 2 ::col 4}))))
(comment (pr-str (text (delete (buffer "aa\nbb\ncc") {::row 1 ::col 2} {::row 3 ::col 2}))))
(comment (pr-str (text (delete (buffer "aa\n\nbb\ncc") {::row 1 ::col 1} {::row 2 ::col 1}))))
(comment (pr-str (text (delete (buffer "aa\n\nbb\ncc") {::row 2 ::col 1} {::row 4 ::col 2}))))
(comment (pr-str (text (delete (buffer "aaaaS\nK\nTbbbb\nbbbb") {::row 1 ::col 1} {::row 2 ::col 0}))))




(defn delete-region
  [buf r]
  (if r
    (delete buf (first r) (second r))
    buf))

(comment (text (delete-region (buffer "aaaa") [{::row 1 ::col 2} {::row 1 ::col 4}])))
(comment (pr-str (text (delete-region (buffer "aa\naa\naa") [{::row 1 ::col 2} {::row 2 ::col 1}]))))

(defn shrink-region
  "Narrow in the given region in both ends"
  [buf r]
  (when r
    (let [p1 (first r)
          p2 (second r)]
      (if (= (point-compare p1 p2) -1)
        [(next-point buf p1) (previous-point buf p2)]
        [(previous-point buf p1) (next-point buf p2)]))))

(defn delete-backward
  [buf]
  (cond (> (-> buf ::cursor ::col) 1) (-> buf left delete-char)
        (= (-> buf ::cursor ::row) 1) buf
        true (let [v (-> buf ::lines (get (dec (-> buf ::cursor ::row))))]
               (-> buf
                   (delete-line (-> buf ::cursor ::row))
                   (update-in [::lines (- (-> buf ::cursor ::row) 2)] #(into [] (concat % v)))
                   (assoc ::cursor {::row (dec (-> buf ::cursor ::row)) ::col (inc (col-count buf (dec (-> buf ::cursor ::row))))})))))

(comment
  (let [buf (buffer "abcd\nxyz")]
    (-> buf
        ;(insert-char 2 4 \k)
        (insert-char \1)
        (insert-char \2)
        left
        (insert-char \newline)
        (insert-char \l)
        right
        (insert-char \m)
        text)))

(defn delete-to-line-end
  "Delete content form the cursor to the end of the line."
  [buf]
  (left (delete-region buf [(buf ::cursor)
                            {::row (-> buf ::cursor ::row)
                             ::col (col-count buf (-> buf ::cursor ::row))}])))

(defn clear
  [buf]
  (assoc buf ::lines [[]]
             ::cursor {::row 1 ::col 1} 
             ::mem-col 1))

(defn split-buffer
  ([buf p]
   (cond (= p (start-point buf)) [(clear buf) buf]
         (= (point-compare p (end-point buf)) 1) [buf (buffer "")]
         true [(delete-region buf [p (end-point buf)])
               (delete-region buf [(start-point buf) {::row (p ::row) ::col (max 0 (dec (p ::col)))}])]))
  ([buf] (split-buffer buf (buf ::cursor))))

(comment (map text (split-buffer (buffer "aaaaSTbbbb\nbbbb") {::row 1 ::col 6})))
(comment (map text (split-buffer (buffer "aaaaS\nK\nTbbbb\nbbbb") {::row 2 ::col 1})))
(comment (map text (split-buffer (buffer "aa") {::row 1 ::col 1})))
(comment (map text (split-buffer (buffer "aa") {::row 2 ::col 1})))
(comment (map text (split-buffer (buffer "aa\n\nccc") {::row 2 ::col 1})))


(defn append-buffer
  [buf buf1]
  (-> buf
      (update-in [::lines (dec (line-count buf))] #(into [] (concat % (first (buf1 ::lines)))))
      (update ::lines #(into [] (concat % (rest (buf1 ::lines)))))
      (set-dirty true)))

(comment (pr-str (text (append-buffer (buffer "aaa\nbbb") (buffer "ccc\ndddd")))))
(comment (pr-str (text (append-buffer (buffer "aaa\n") (buffer "bbb")))))
(comment (pr-str (text (append-buffer (buffer "aaa") (buffer "bbb\n\n")))))
(comment (pr-str (text (buffer "bbb\n\n"))))
(comment (pr-str (text (append-buffer (buffer "aaa") (buffer "\nbbb")))))

(defn insert-buffer
  ([buf p buf0]
   (let [[b1 b2] (split-buffer buf p)
         hidden ((adjust-hidden-rows buf (inc (p ::row)) (dec (line-count buf0))) ::hidden-lines)]
     (assoc
       (append-buffer b1 (append-buffer buf0 b2))
       ::hidden-lines hidden)))
  ([buf buf0]
   (insert-buffer buf (buf ::cursor) buf0)))

(comment (pr-str (text (insert-buffer (buffer "aaa\n\nbbb") (buffer "")))))
(comment (-> (buffer "aaa") end-of-buffer (insert-buffer (buffer "bbb")) text))
(comment (-> (buffer "aaa\n") end-of-buffer (insert-buffer (buffer "bbb")) text))
(comment (-> (buffer "aaa\n") :liq.buffer/cursor pr-str println))
(comment (-> (buffer "aaa\n") end-of-buffer :liq.buffer/cursor pr-str println))
(comment (-> (buffer "aaa\nb") end-of-buffer :liq.buffer/cursor pr-str println))
(comment (-> (buffer "aaa\n") end-of-buffer ::cursor))
(comment (-> (buffer "aaa\n") end-of-buffer (insert-buffer (buffer "bbb")) text))
(comment (text (insert-buffer (buffer "aaaaabbbbb") {::row 1 ::col 6} (buffer "cccc"))))
(comment (text (insert-buffer (buffer "aaaaa\n\nbbbbb") {::row 2 ::col 1} (buffer "cccc"))))
(comment (text (insert-buffer (buffer "aaaaabbbbb") (buffer "cccc"))))
(comment (get-row (insert-buffer (buffer "aaaaabbbbb") (buffer "cccc"))))
(comment (pr-str (text (insert-buffer (buffer "") (buffer "")))))
(comment (text (insert-buffer (buffer "") (buffer ""))))
(comment (pr-str (text (insert-buffer (buffer "aaa") (buffer "bbb\n")))))

(defn insert-string
  [buf text]
  (insert-buffer buf (buffer text)))

(defn insert-at-line-end
  [buf]
  (-> buf
      set-insert-mode
      end-of-line
      right))

(defn first-non-blank
  [buf]
  (let [l (line buf)
        col (+ (or (count (re-find #"\s*" l)) 0) 1)]
    (-> buf
        (assoc-in [::cursor ::col] col))))

(defn insert-at-beginning-of-line
  [buf]
  (-> buf
      set-insert-mode
      first-non-blank))

(defn join-lines
  [buf]
  (if (= (-> buf ::cursor ::row) (-> buf ::lines count))
    buf
    (-> buf
        down
        beginning-of-line
        delete-backward
        (adjust-hidden-rows (-> buf ::cursor ::row) -1))))

(defn join-lines-space
  [buf]
  (if (= (-> buf ::cursor ::row) (-> buf ::lines count))
    buf
    (let [b1 (if (= (-> buf end-of-line get-char) \space)
               buf
               (-> buf set-insert-mode end-of-line right (insert-char \space)))
          col (-> buf down first-non-blank ::cursor ::col)
          row (inc (-> buf ::cursor ::row))]
    (-> b1
        (delete {::row row ::col 0} {::row row ::col (dec col)})
        delete-backward
        left))))


(defn set-attribute
  [buf row col attr value]
  (if (get-char buf row col)
    (assoc-in buf [::lines (dec row) (dec col)] {attr value})
    buf))

(defn match-before
  [buf p0 re]
  (loop [p (previous-point buf p0)]
    (when p
      (if (re-find re (str (get-char buf p)))
        p
        (recur (previous-point buf p))))))

(comment
  (previous-point (buffer "aaa bbb ccc") {::row 1 ::col 8})
  (previous-point (buffer "aaa bbb ccc") {::row 1 ::col 1})
  (match-before (buffer "aaa bbb ccc") {::row 1 ::col 8} #"a"))


(defn paren-match-before
  "(abc (def) hi|jk)"
  [buf p0 paren]
  (let [pmatch {\( \) \) \( \{ \} \} \{ \[ \] \] \[ \" \"}] ;"
    (loop [p p0 stack (list (pmatch paren))]
      (when p
        (let [c (get-char buf p)
              nstack (cond (nil? c) stack
                           (= (pmatch c) (first stack)) (rest stack)
                           (some #{c} (keys pmatch)) (conj stack c)
                           true stack)]
          (if (empty? nstack)
            p
            (recur (previous-point buf p) nstack)))))))

(defn paren-match-after
  "(abc (def) hi|jk)"
  [buf p0 paren]
  (let [pmatch {\( \) \) \( \{ \} \} \{ \[ \] \] \[ \" \"}] ;"
    (loop [p p0 stack (list (pmatch paren))]
      (when p
        (let [c (get-char buf p)
              nstack (cond (nil? c) stack
                           (= (pmatch c) (first stack)) (rest stack)
                           (some #{c} (keys pmatch)) (conj stack c)
                           true stack)]
          (if (empty? nstack)
            p
            (recur (next-point buf p) nstack)))))))

;; Regions
;; =======

(defn paren-region
  ([buf p]
   (let [p0 (paren-match-before buf (if (= (get-char buf p) \)) (previous-point buf p) p) \()
         p1 (when p0 (paren-match-after buf (next-point buf p0) \)))]
     (when p1 [p0 p1])))
  ([buf] (paren-region buf (buf ::cursor))))

(defn bracket-region
  ([buf p]
   (let [p0 (paren-match-before buf (if (= (get-char buf p) \]) (previous-point buf p) p) \[)
         p1 (when p0 (paren-match-after buf (next-point buf p0) \]))]
     (when p1 [p0 p1])))
  ([buf] (bracket-region buf (buf ::cursor))))

(defn brace-region
  ([buf p]
   (let [p0 (paren-match-before buf (if (= (get-char buf p) \}) (previous-point buf p) p) \{)
         p1 (when p0 (paren-match-after buf (next-point buf p0) \}))]
     (when p1 [p0 p1])))
  ([buf] (brace-region buf (buf ::cursor))))

(comment (paren-region (buffer "(asdf)")))
(comment (bracket-region (buffer "[asdf]")))

(defn quote-region
  ([buf p]
   (let [p0 (paren-match-before buf (if (= (get-char buf p) \") (previous-point buf p) p) \") ;"
         p1 (when p0 (paren-match-after buf (next-point buf p0) \"))] ;" 
     (when p1 [p0 p1])))
  ([buf] (quote-region buf (buf ::cursor))))

(comment (quote-region (-> (buffer "(\"asdf\")") right right)))

(defn line-region
  "Line at point as region."
  ([buf p]
   (when (<= (p ::row) (line-count buf))
     [(assoc p ::col 1) (assoc p ::col (col-count buf (p ::row)))]))
  ([buf] (line-region buf (buf ::cursor)))) 

(defn eol-region
  ([buf p]
   (when (<= (p ::row) (line-count buf))
     [p (assoc p ::col (col-count buf (p ::row)))]))
  ([buf] (eol-region buf (buf ::cursor)))) 


(comment (line-region (buffer "abc\ndefhi") {::row 2 ::col 2}))
  

(defn paren-matching-region
  "Forward until first paren on given row.
  Depending on type and direction move to corresponding
  paren.
  Returns nil if there is no hit."
  [buf p]
  (let [pbegin (start-point buf)
        pend (end-point buf)
        ncol (fn [p0] (update p0 ::col inc))
        pmatch {\( \) \) \( \{ \} \} \{ \[ \] \] \[}
        p1 (loop [p0 p]
             (cond (nil? (get-char buf p0)) nil 
                   (pmatch (get-char buf p0)) p0
                   true (recur (ncol p0))))]
    (when p1
      (let [par1 (get-char buf p1)
            par2 (pmatch par1)
            direction (if (#{\( \[ \{} par1) next-point previous-point)]
        (loop [p0 (direction buf p1) n 1]
          (when p0
            (let [c (get-char buf p0)
                  nnext (cond (= c par1) (inc n)
                              (= c par2) (dec n)
                              true n)]
              (cond (= nnext 0) [p1 p0]
                    (= p0 start-point) nil
                    (= p0 end-point) nil
                    true (recur (direction buf p0) nnext)))))))))

(comment (paren-region (buffer "ab (cde\naaa bbb (ccc))") {::row 2 ::col 5}))
(comment (paren-region (buffer "ab (cde\naaa bbb (ccc))") {::row 2 ::col 5}))
(comment (pr-str (paren-region (buffer "ab cde\naaa bbb ccc") {::row 2 ::col 3})))

(defn move-matching-paren
  [buf]
  (let [r (paren-matching-region buf (buf ::cursor))]
    (if r
      (update-mem-col (assoc buf ::cursor (second r)))
      buf)))



(defn search
  ""
  ([buf w]
   (let [b (assoc buf ::search-word w)
         regex (re-pattern w)
         l (line b)
         s (subs l (min (-> b ::cursor ::col) (count l)))
         res (str/split s regex 2)]
     (if (>= (count res) 2)
       (right b (inc (count (first res))))
       (loop [row (inc (-> b ::cursor ::row))]
         (let [s (line b row)]
           (cond (re-find regex s) (assoc b ::cursor {::row row ::col (inc (count (first (str/split s regex 2))))})
                 (>= row (line-count b)) b
                 true (recur (inc row))))))))
  ([buf] (search buf (buf ::search-word))))

(comment (search (buffer "aaaa bbbb") "b"))

 

(defn sexp-at-point
  ([buf p]
   (let [p0 (paren-match-before buf (if (= (get-char buf p) \)) (previous-point buf p) p) \()
         p1 (when p0 (paren-match-after buf (next-point buf p0) \)))]
     (when p1 (text buf p0 p1))))
  ([buf] (sexp-at-point buf (buf ::cursor))))

(defn word-beginnings
  "TODO Not used"
  [text]
  (reduce
    #(conj %1 (+ (last %1) %2))
    [0]
    (map count (drop-last (str/split text #"(?<=\W)\b")))))

(defn beginning-of-word
  ([buf]
   (loop [b (or (previous-point buf) buf)]
     (let [p (b ::cursor)
           c (str (get-char b))
           is-word (re-matches #"\w" c)]
       (cond (= p {::row 1 ::col 1}) b 
             (and is-word (= (p ::col) 1)) b
             (= (p ::col) 1) (recur (previous-point b))
             (and is-word (re-matches #"\W" (str (get-char (left b))))) b
             true (recur (left b))))))
  ([buf n] (nth (iterate beginning-of-word buf) n)))


(defn end-of-word
  ([buf]
   (loop [b (or (next-point buf) buf)]
     (let [p (b ::cursor)
           rows (line-count b)
           cols (col-count b (p ::row))
           c (str (get-char b))
           is-word (re-matches #"\w" c)]
       (cond (and (= rows 1) (= cols 0)) b
             (= p {::row rows ::col cols}) b 
             (and is-word (= (p ::col) cols)) b
             (= (p ::col) cols) (recur (next-point b))
             (and is-word (re-matches #"\W" (str (get-char (right b))))) b
             true (recur (right b))))))
  ([buf n] (nth (iterate end-of-word buf) n)))

(defn end-of-word-region
  ([buf]
   [(buf ::cursor) ((end-of-word buf) ::cursor)])
  ([buf n]
   [(buf ::cursor) ((end-of-word buf n) ::cursor)]))


(defn end-of-word-ws
  ([buf]
   (loop [b (or (next-point buf) buf)]
     (let [p (b ::cursor)
           rows (line-count b)
           cols (col-count b (p ::row))
           c (str (get-char b))
           is-word (re-matches #"\S" c)]
       (cond (= p {::row rows ::col cols}) b 
             (and is-word (= (p ::col) cols)) b
             (= (p ::col) cols) (recur (next-point b))
             (and is-word (re-matches #"\s" (str (get-char (right b))))) b
             true (recur (right b))))))
  ([buf n] (nth (iterate end-of-word-ws buf) n)))

(defn end-of-word-ws-region
  ([buf]
   [(buf ::cursor) ((end-of-word-ws buf) ::cursor)])
  ([buf n]
   [(buf ::cursor) ((end-of-word-ws buf n) ::cursor)]))


(defn word-region
  ([buf]
   (let [b1 (-> buf left end-of-word)]
     [(-> b1 beginning-of-word ::cursor)
             (-> b1 ::cursor)]))
  ([buf p]
   (word-region (assoc buf ::cursor p))))

(comment (word-region (buffer "aaa bbb ccc") {:liq.buffer/row 1 :liq.buffer/col 5}))
(comment (word-region (buffer "aaa bb: ccc") {:liq.buffer/row 1 :liq.buffer/col 5}))
(comment (word-region (buffer "aaa bb/ ccc") {:liq.buffer/row 1 :liq.buffer/col 5}))
(comment (word-region (buffer "aaa bbb ccc")))

(defn char-region
  ([buf p] [p p])
  ([buf] (char-region (buf ::cursor))))

(comment (hide-region (buffer "abc\ndef") [{::row 1 ::col 1} {::row 2 ::col 2}]))

(defn word-forward
  ([buf]
   (loop [b (or (next-point buf) buf)]
     (let [p (b ::cursor)
           rows (line-count b)
           cols (col-count b (p ::row))
           c (str (get-char b))
           is-word (re-matches #"\w" c)]
       (cond (= p {::row rows ::col cols}) b 
             (and is-word (= (p ::col) 1)) b
             (and is-word (re-matches #"\W" (str (get-char (left b))))) b
             true (recur (next-point b))))))
  ([buf n] (nth (iterate word-forward buf) n)))

(defn word-forward-ws
  ([buf]
   (loop [b (or (next-point buf) buf)]
     (let [p (b ::cursor)
           rows (line-count b)
           cols (col-count b (p ::row))
           c (str (get-char b))
           is-word (re-matches #"\S" c)]
       (cond (= p {::row rows ::col cols}) b 
             (and is-word (= (p ::col) 1)) b
             (and is-word (re-matches #"\s" (str (get-char (left b))))) b
             true (recur (next-point b))))))
  ([buf n] (nth (iterate word-forward-ws buf) n)))


(defn calculate-wrapped-row-dist
  [buf cols row1 row2]
  ;; todo subtract number of hidden lines between row1 and row2
  (reduce #(if (row-hidden? buf %2)
             %1
             (+ %1 1 (quot (dec (col-count buf %2)) cols)))
          0 (range row1 row2))) 

(defn recalculate-tow
  "This is a first draft, which does not handle edge
  cases with very long lines and positioning logic."
  [buf rows cols tow1]
  (let [row (-> buf ::cursor ::row)
        towrow (tow1 ::row)]
    (cond (< row towrow) (assoc tow1 ::row row)
          ;(> (- row towrow) rows)
          (> (visible-rows-count buf towrow row) rows)
            ;(recalculate-tow buf rows cols (assoc tow1 ::row (- row rows)))
            (recalculate-tow buf rows cols
                         (assoc tow1 ::row (nth (iterate #(previous-visible-row buf %) row) (dec rows))))
          (> (calculate-wrapped-row-dist buf cols (tow1 ::row) (+ row 1)) rows)
            (recalculate-tow buf rows cols (update tow1 ::row inc))
          true tow1)))

(comment (nth (iterate #(previous-visible-row (buffer "aaa\nbbb\nccc\ndddd\neee") %) 1) 3))

(defn update-tow
  [buf]
  (let [w (buf ::window)]
    (assoc buf ::tow (recalculate-tow buf (w ::rows) (w ::cols) (buf ::tow)))
    ;(assoc buf ::tow {::row 1 ::col 1})
    ))

(comment (update-tow (buffer "ab[[cd]\nx[asdf]yz]")))

;; Emacs has two stages:
;; 1. Where comments and strings are highlighted
;;    Define comment start and comment end
;;    Define string delimiter and escape-char
;; 2. Where keywords are highlighted. These are matched by regexes outside strings
;; https://medium.com/@model_train/creating-universal-syntax-highlighters-with-iro-549501698fd2
;; https://github.com/atom/language-clojure/blob/master/grammars/clojure.cson
;; (re-find #"(?<!\\)(\")" "something \"a string\" else")
;; (re-find #"(?<=(\s|\(|\[|\{)):[\w\#\.\-\_\:\+\=\>\<\/\!\?\*]+(?=(\s|\)|\]|\}|\,))" "abc :def hij :ppp ")

;; Not use regex, but functions, which might be regex! str/index-of or count str/split

;(defn apply-syntax-hl
;  [buf hl]
;  ;; TODO Now only highlighting row 1 as experiment - In progress
;  (loop [b buf col 1 keyw :plain]
;    (if (> col (col-count b 1))
;      b
;      (let [c (get-char b 1 col)
;            keywn (cond (and (= keyw :plain) (= c "\"")) :string
;                        (and (= keyw :plain) (= c ";")) :comment 
;                        (and (= keyw :string) (= c "\"")) :
;  buf))

(comment

  (let [buf (buffer "ab[[cd]\nx[asdf]yz]")]
    (paren-match-before buf {::row 2 ::col 8} \[))

  (let [buf (buffer "aaa bbb ccc")]
    (beginning-of-word (assoc buf ::cursor {::row 1 ::col 8})))

  (let [buf (buffer "aaa bbb ccc")]
    (match-before buf {::row 1 ::col 8} #"a"))

  (pr-str (line (buffer "") 2))
  
  (let [buf (buffer "ab[[cd]\nx[asdf]yz]")]
    (paren-match-before buf {::row 1 ::col 3} \]))

  (let [buf (buffer "ab((cd)\nx(asdf)yz)")]
    (paren-match-before buf {::row 2 ::col 5} \)))

  (let [buf (buffer "ab((cd)\nx(asdf)yz)")]
    (sexp-at-point buf {::row 2 ::col 2}))

  (let [buf (buffer "abcd\nxyz")]
    (-> buf
        (set-char 5 6 \k)
        text)))


(comment
  (let [buf (buffer "abcd\nxyz")]
    (-> buf
        right
        text)))


(comment
  (-> (buffer "") set-insert-mode (line 1) pr-str)
  (-> (buffer "") set-insert-mode (insert-char \a) set-normal-mode (line 1) pr-str)
  (-> (buffer "") set-insert-mode (insert-char \a) set-normal-mode ::cursor)
  (-> (buffer "") set-insert-mode (insert-char \a) set-normal-mode left right ::cursor)
  (-> (buffer "") set-insert-mode (insert-char \a) set-normal-mode left ::cursor)
  (-> (buffer "abcd\nxyz") (right 3) down)
  (= (-> (buffer "abcd\nxyz") (right 3) down get-char) \z)
  (-> (buffer "abcd\nxyz") (insert-char 4 5 \k) text)
  (-> (buffer "abcd\nxyz") append-line text)

  (text (buffer "abcd\nxyz"))
  (end-of-line (buffer ""))
  (beginning-of-buffer (buffer ""))
  (insert-char (buffer "") \a)
  (insert-char (buffer "") "a")
)
