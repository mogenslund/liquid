(ns dk.salza.liq.slider
  "The slider is a basic construction resembling the most
  fundamental actions of a text edtior.

  The slider is immutable, so every slider function in this
  namespace will take a slider as input together with some
  parameters and evaluate to a new slider.

  By having the slider as first parameter a series of actions
  can be performed by using the threading operator \"->\",
  like:
 
  (->
    sl
    (end-of-line)
    (forward-char 3))

  Some of the basic operations are:

  * Moving cursor
  * Inserting and removing content
  * Setting marks and moving cursor to marks
  * Marking regions

  The sliderutil contains more slider related functions,
  which are more complex and usually composed of the basic
  functions in this file."
  (:require [clojure.string :as str]))

(defn create
  "Creates a new slider with given text as text.
  The point will be placed at the beginning.

  A slider basically consists of two lists:

  before: A list of characters (strings) before the point,
          where the first element is the character just before the point.
  after:  A list of characters (strings) after the point,
          where the first element is the character just after the point.

  So moving the point is more or less to take characters from one
  of the lists and put into the other.

  This text: abc|def

  will look like this:

      before = (c b a), after = (d e f).

  Moving the curser to the right will result in:

      before = (d c b a), after = (e f)."
  ([text]
    (let [after (if (string? text) (map str text) text)] ; Creating with a list '("a" "b") should also work
     {::before ()     ; The list of characters before the cursor in reverse order
      ::after after   ; The list of characters after the cursor in normal order
      ::point 0       ; The current point (cursor position). Starts at 0, the begining of the slider
      ::linenumber 1  ; Meta information for fast retrievel of the current line number
      ::totallines (inc (count (filter #(= % "\n") after))) ; Only to make the end function perform optimal
      ::dirty false
      ::marks {}}))   ; A map of named positions, like "selection" -> 18.
                      ; The positions are pushed, when text is insertet
                      ; strictly before the mark.
  ([] (create "")))

(defn slider?
  "Returns true if the input has shape/properties
  like a slider."
  [sl]
  (and (map? sl) (sl ::before)))

(defn set-dirty
  ([sl dirty]
   (assoc sl ::dirty dirty))
  ([sl]
   (assoc sl ::dirty true)))

(defn dirty?
  [sl]
  (sl ::dirty))

(defn clear
  "Erases everything in the slider,
  content and marks."
  [sl]
  (-> (create) set-dirty))

(defn beginning
  "Moves the point (cursor) to the beginning of the slider."
  [sl]
  (assoc sl
   ::before ()
   ::after (concat (reverse (sl ::before)) (sl ::after))
   ::point 0
   ::linenumber 1))

(defn end
  "Moves the point to the end of the slider."
  [sl]
  (let [tmp (concat (reverse (sl ::after)) (sl ::before))]
    (assoc sl
     ::before tmp
     ::after ()
     ::point (count tmp)
     ::linenumber (sl ::totallines))))


(defn get-char
  "Returns the first character after the point.
  If point is at the end of the slider, nil will
  be returned."
  [sl]
  (let [c (first (sl ::after))]
    (cond (string? c) c
          c (or (c :char) c)
          :else nil)))

(defn hidden?
  "Checks if current position is a hidden
  region."
  [sl]
  (slider? (first (sl ::after))))

(defn set-meta
  "Set a meta value on the current char.
  If char is a string it will be converted to a map."
  [sl key val]
  (let [c (first (sl ::after))
        c1 (cond (string? c) {:char c key val}
                 (map? c) (assoc c key val)
                 :else c)]
    (if c1
      (assoc sl
        ::after (conj (rest (sl ::after)) c1))
      sl)))

(defn get-meta
  "Get a meta value from a char.
  If the value does not exist, nil will be
  returned."
  [sl key]
  (let [c (first (sl ::after))]
    (when (map? c) (c key))))

(defn is-newline?
  "Check if \\n or {:char \\n}"
  [c]
  (not (or (and (string? c) (not= c "\n"))
       (and (map? c) (not= (c :char) "\n")))))

(defn beginning?
  "True if and only if the point is at the
  beginning of the slider."
  [sl]
  (empty? (sl ::before)))

(defn end?
  "True if and only if the point is at the
  end of the slider."
  [sl]
  (empty? (sl ::after)))

(defn look-behind
  "Returns char behind the cursor given amount back.
  If amount = 1 the char right behind the cursor will be
  returned.
  Non strings will be filtered away.
  If there is no result, nil is returned."
  [sl amount]
  (first (drop (- amount 1) (sl ::before))))

(defn look-ahead
  "Returns char after the cursor given amount forward.
  If amount = 0 the char right after the cursor will be
  returned.
  Non strings will be filtered away.
  If there is no result, nil is returned."
  [sl amount]
  (first (drop amount (sl ::after))))

(defn string-ahead
  "Returns next amount of chars as string.
  Non string will be filtered away."
  [sl amount]
  (str/join "" (take amount (sl ::after))))
  

(defn get-point
  "Returns the point. If at the beginning of the
  slider the result is 0. If at the end of the
  slider the result is the number of characters."
  [sl]
  (sl ::point))


(defn get-linenumber
  "Return the linenumber of the point.
  It will always be equal to one more
  than the number of newline characters
  in ::before."
  [sl]
  (sl ::linenumber))

(defn get-visible-content
  "This is not really in use yet, since there
  is not yet a hide lines functionality."
  [sl]
  (let [tostr (fn [& chs] (apply str (map #(if (string? %) % "¤") chs)))]
    (str (apply tostr (reverse (sl ::before))) (apply tostr (sl ::after)))))  

(defn slide-marks
  "This function will move marks strictly after
  point with the given amount.
  Marks at point will not be moved.
  When text is inserted, marks should be
  moved accordingly."
  [marks point amount]
  (let [ks (keys (select-keys marks (for [[k v] marks :when (> v point)] k)))]
    (reduce #(assoc %1 %2 (max point (+ (%1 %2) amount))) marks ks)))

(defn left
  "Moves the point to the left the given amount of times.
  So moving one character left is achieved with
  (left sl 1)."
  ([sl]
   (let [c (first (sl ::before))]
     (if (beginning? sl)
         sl
         (assoc sl
           ::before (rest (sl ::before))
           ::after (conj (sl ::after) c)
           ::point (dec (sl ::point))
           ::linenumber (- (sl ::linenumber)
                           (cond (is-newline? c) 1
                                 (slider? c) (- (c ::totallines) 1)
                                 true 0))))))
  ([sl amount]
   (loop [s sl n amount]
     (if (<= n 0)
       s
       (recur (left s) (dec n))))))
         

(defn right
  "Moves the point to the right (forward) the given amount of times."
  ([sl]
   (let [c (first (sl ::after))]
     (if (end? sl)
         sl
         (assoc sl
           ::before (conj (sl ::before) c)
           ::after (rest (sl ::after))
           ::point (inc (sl ::point))
           ::linenumber (+ (sl ::linenumber)
                           (cond (is-newline? c) 1
                                 (slider? c) (- (c ::totallines) 1)
                                 true 0))))))
  ([sl amount]
   (loop [s sl n amount]
     (if (<= n 0)
       s
       (recur (right s) (dec n))))))

(defn set-point
  "Moves point the the given location.
  Not further than beginning of the slider
  and the end of the slider."
  [sl newpoint]
  (if (> newpoint (sl ::point))
    (right sl (- newpoint (sl ::point)))
    (left sl (- (sl ::point) newpoint))))


(defn insert
  "Insert text at the point. The point will be
  moved to the end of the inserted text."
  [sl text]
    (let [n (count text)
          linecount (count (filter #(= % \newline) text))]
      (assoc sl
        ::before (concat (map str (reverse text)) (sl ::before))
        ::point (+ (sl ::point) n)
        ::linenumber (+ (sl ::linenumber) linecount)
        ::totallines (+ (sl ::totallines) linecount)
        ::dirty true
        ::marks (slide-marks (sl ::marks) (+ (sl ::point) n -1) n))))

(defn insert-newline
  "Special function to insert newline without too
  much overhead."
  [sl]
  (assoc sl
    ::before (conj (sl ::before) "\n")
    ::point (inc (sl ::point))
    ::linenumber (inc (sl ::linenumber))
    ::totallines (inc (sl ::totallines))
    ::dirty true
    ::marks (slide-marks (sl ::marks) (sl ::point) 1)))

(defn insert-space
  "Special funcon to insert a space without too
  much overhead."
  [sl]
  (assoc sl
    ::before (conj (sl ::before) " ")
    ::point (inc (sl ::point))
    ::dirty true
    ::marks (slide-marks (sl ::marks) (sl ::point) 1)))

(defn insert-subslider
  [sl subsl]
  (assoc sl
    ::before (conj (sl ::before) subsl)
    ::point (inc (sl ::point))
    ::marks (slide-marks (sl ::marks) (sl ::point) 1)
    ::linenumber (+ (sl ::linenumber) (subsl ::totallines) -1)
    ::dirty true
    ::totallines (+ (sl ::totallines) (subsl ::totallines) -1)))

(defn delete
  "Deletes amount of characters to the left of
  the cursor. So delete 3 of
  aaabbb|ccc wil result in
  aaa|ccc."
  [sl amount]
  (let [tmp (take amount (sl ::before))
        linecount (+ (count (filter is-newline? tmp)) (apply + (map #(- (% ::totallines) 1) (filter slider? tmp))))
        n (count tmp)]
    (when (= (count (filter list? tmp)) 0) ; Only delete if not hidden
      (assoc sl
        ::before (drop n (sl ::before))
        ::point (- (sl ::point) n)
        ::linenumber (- (sl ::linenumber) linecount)
        ::totallines (- (sl ::totallines) linecount)
        ::dirty true
        ::marks (slide-marks (sl ::marks) (- (sl ::point) n) (* -1 n))))))

(defn wrap
  "Wrap all lines. Cursor will be at end."
  [sl columns]
  (loop [sl1 (beginning sl) slsp nil col 0]
    (cond (end? sl1) sl1
          (= (get-char sl1) "\n") (recur (right sl1 1) nil 0)
          (= col columns) (recur (insert-newline (or slsp sl1)) nil 0)
          (= (get-char sl1) " ") (let [sl2 (right sl1 1)] (recur sl2 sl2 (inc col)))
          :else (recur (right sl1 1) slsp (inc col)))))

(defn pad-right
  "Pad to the right with spaces"
  [sl columns]
  (loop [sl1 (beginning sl) col 0]
    (cond (and (end? sl1) (>= col columns)) sl1
          (and (= (get-char sl1) "\n") (>= col columns)) (recur (right sl1 1) 0)
          (= (get-char sl1) "\n") (recur (insert-space sl1) (inc col))
          :else (recur (right sl1 1) (inc col)))))

(defn set-mark
  "Sets a named mark at the point.
  When inserting or deleting text strectly left to
  the mark, the mark will be moved
  accordingly."
  [sl name]
  (assoc-in sl [::marks name] (sl ::point)))

(defn get-mark
  "The position of the mark
  with the given name."
  [sl name]
  ((sl ::marks) name))

(defn remove-mark
  "Removes the mark with the given name."
  [sl name]
  (update sl ::marks dissoc name))

(defn clear-marks
  "Removes all marks."
  [sl]
  (assoc sl ::marks {}))

(defn point-to-mark
  "Moves the point to the mark
  with the given name.
  If the mark does not exist nothing
  is changed."
  [sl name]
  (if (get-mark sl name)
      (set-point sl (get-mark sl name))
      sl))

(defn before
  "Returns a slider with content before cursor."
  [sl]
  (assoc sl
    ::after ()
    ::totallines (sl ::linenumber)
    ::marks (into {} (remove #(> (second %) (sl ::point)) (sl ::marks)))))

(defn after
  "Returns a slider with content after cursor."
  [sl]
  (assoc sl
    ::before ()
    ::linenumber 1
    ::totallines (- (sl ::totallines) (sl ::linenumber) -1)
    ::point 0
    ::marks (slide-marks (into {} (remove #(< (second %) (sl ::point)) (sl ::marks)))
                         0
                         (* -1 (sl ::point)))))

(defn insert-slider
  "Insert second slider into first.
  Point is moved to match second slider.
  Marks are lost"
  [sl1 sl2]
  {::before (concat (sl2 ::before) (sl1 ::before))
   ::after (concat (sl2 ::after) (sl1 ::after))
   ::point (+ (sl1 ::point) (sl2 ::point))
   ::linenumber (+ (sl1 ::linenumber) (sl2 ::linenumber) -1)
   ::totallines (+ (sl1 ::totallines) (sl2 ::totallines) -1)
   ::dirty true
   ::marks {}})



(defn right-until
  "Moves the cursor forward, until for the current char:
  (pred char) is true.
  The cursor will be placed just before the
  character. The function only matches single characters, not
  character sequences!
  If there is no match, the cursor will move all the way to the
  end of the slider.
  Example (cursor = ^):
    aaacde^aacf   -- right-until c -->   aacdeaa^cf."
  [sl pred] ; (re-matches #"(a|b)" "a")
  (loop [s sl]
    (let [c (get-char s)]
      (if (or (end? s) (pred c))
        s
        (recur (right s 1))))))

(defn left-until
  "Moves the cursor backward, until for the current char:
  (pred char) is true.
  The cursor will be places just before the
  character. The function only mathces single characters, not
  character sequences!
  If there is no match, the cursor will move all the way to the
  beginning of the slider.
  Example (cursor = ^):
    aaacde^aacf   -- left-until c -->   aa^cdeaacf."
  [sl pred] ; (re-matches #"(a|b)" "a")
  (loop [s (if (end? sl) (left sl 1) sl)]
    (let [c (get-char s)]
      (if (or (beginning? s) (pred c))
        s
        (recur (left s 1))))))

(defn mark-paren-start
  "Marks the paren-start of the
  current s-exp. In this case:
  aaa (aaa (aa)  a|aa
  The first paren start is selected."
  [sl name]
  (loop [sl0 (-> sl (remove-mark name) (set-mark "mark-paren-curser"))
         ch (if (= (get-char sl) ")") "" (get-char sl))
         level 0]
    (cond (and (= ch "(") (= level 0)) (-> sl0 (set-mark name) (point-to-mark "mark-paren-curser"))
          (beginning? sl0) sl
          :else (recur (left sl0 1)
                       (get-char (left sl0 1))
                       (cond (= ch "(") (dec level)
                             (= ch ")") (inc level)
                             :else level)))))

(defn mark-paren-end
  "Marks the paren-end of the
  current s-exp. In this case:
  aa (aa (aa|aa(aa))
  The last paren will be selected."
  [sl name]
  (loop [sl0 (-> sl (remove-mark name) (set-mark "mark-paren-curser"))
         ch (if (= (get-char sl) "(") "" (get-char sl))
         level 0]
    (cond (and (= ch ")") (= level 0)) (-> sl0 right (set-mark name) (point-to-mark "mark-paren-curser"))
          (end? sl0) sl
          :else (recur (right sl0 1)
                       (get-char (right sl0 1))
                       (cond (= ch "(") (inc level)
                             (= ch ")") (dec level)
                             :else level)))))

(defn select-sexp-at-point
  "Selects the smallest valid s-expression containing
  the point (cursor position). The function take into
  account that the parenthesis should be balanced."
  [sl]
  (let [sel (get-mark sl "selection")
        sl0 (-> sl (mark-paren-start "paren-start") (mark-paren-end "paren-end"))]
    (if (and (get-mark sl0 "paren-start") (get-mark sl0 "paren-end"))
      (if (= sel (get-mark sl0 "paren-end"))
        (-> sl0 (point-to-mark "paren-start") (set-mark "selection") (point-to-mark "paren-end") left)
        (-> sl0 (point-to-mark "paren-end") (set-mark "selection") (point-to-mark "paren-start")))
      sl)))

(defn highlight-sexp-at-point
  "Setting marks hl0 and hl1 on parenthesis matching the
  current s-expression for a view to use to highlight the
  the boundries of the s-expression."
  [sl]
  (if (get-mark sl "hl0")
    (-> sl
      (remove-mark "hl0")
      (remove-mark "hl1"))
    (-> sl
      (set-mark "cursor")
      (mark-paren-start "hl0")
      (mark-paren-end "hl1")
      (point-to-mark "cursor"))))
  

(defn forward-word
  "Moves the point to beginning of next word or end-of-buffer"
  [sl]
  (-> sl (right-until (partial re-find #"\s")) right (right-until (partial re-find #"\S")))) ; (not (not (re-matches #"\S" "\n")))

(defn end-of-line
  "Moves the point to the end of the line. Right before the
  next line break."
  [sl]
  (right-until sl #(= % "\n")))

(defn beginning-of-line
  "Moves the point to the beginning
  of the current line."
  [sl]
  (loop [sl0 sl]
    (if (or (empty? (sl0 ::before)) (= (first (sl0 ::before)) "\n"))
      sl0
      (recur (left sl0 1)))))

;;; This function might assume the position is a start of line!
(defn forward-line
  ([sl columns]
   (loop [s sl cand nil c 0]
     (cond (= (get-char s) "\n") (right s 1)
           (= c columns) (or cand s) ; If there is a candidate otherwise to current position
           (end? s) s
           :else (let [next (right s 1)]
                   (recur next
                          (if (= (get-char s) " ") next cand)
                          (inc c))))))
  ([sl]
    ;; In this case just to end of buffer or efter next \n
    (-> sl (end-of-line) right)))

(defn get-visual-column
  [sl columns]
  (let [p0 (get-point sl)
        sl0 (beginning-of-line sl)]
    (loop [sl2 (forward-line sl0 columns) sl1 sl0]
      (cond (> (get-point sl2) p0) (- p0 (get-point sl1))
            (end? sl2) (- p0 (get-point sl1))
            (= (get-point sl2) p0) 0
            :else (recur (forward-line sl2 columns) sl2)))))
                   

(defn forward-visual-column
  "Moves the point forward one line, where
  the lines are wrapped. The parameter columns is
  how many chars are allowed on one line. The parameter
  column is the current selected column.
  The visual result of this function is the cursor will
  be located on the same column on the next line, nomatter
  if the line was wrapped or not."
  [sl columns column]
  (let [cur-column (get-visual-column sl columns)]
    (loop [sl0 (-> sl (left cur-column) (forward-line columns)) n 0]
      (if (or (= n column)
              (end? sl0)
              (= (get-char sl0) "\n"))
          sl0
          (recur (right sl0 1) (inc n))))))

(defn backward-visual-column
  "Moves the point backward one line, where
  the lines are wrapped. The parameter columns is
  how many chars are allowed on one line. The parameter
  column is the current selected column.
  The visual result of this function is the cursor will
  be located on the same column on the previous line, nomatter
  if the line was wrapped or not."
  [sl columns column]
  (let [vc (get-visual-column sl columns)
        sl0 (left sl (+ vc 1))
        vc0 (get-visual-column sl0 columns)]
    (if (> vc0 column)
       (left sl0 (- vc0 column))
       sl0)))

(defn get-content
  "The full content of the slider as text."
  [sl]
  (apply str (map #(if (string? %) % (or (% :char) (get-content %))) (-> sl beginning ::after))))

(defn get-region-as-slider
  [sl markname]
  (let [mark (get-mark sl markname)]
    (when mark
      (let [l (if (< mark (get-point sl))
                (reverse (take (- (get-point sl) mark) (sl ::before)))
                (take (- mark (get-point sl)) (sl ::after)))
            total (+ (count (filter is-newline? l)) (apply + (map #(- (% ::totallines) 1) (filter slider? l))) 1)]
        (assoc (create) ::after l ::totallines total)))))

(defn get-region
  "Returns the content between the mark
  with the given name and the point.
  If there is no mark with the given name
  nil is returned."
  [sl markname]
  (when-let [r (get-region-as-slider sl markname)]
    (get-content r)))

(defn delete-region
  "Deletes the region between the given
  mark and the point. If the mark does
  not exist, nothing is deleted."
  [sl markname]
  (if-let [mark (get-mark sl markname)]
    (let [p0 (get-point sl)]
      (-> sl (set-point (max p0 mark))
             (delete (- (max p0 mark) (min p0 mark)))))
    sl))

(defn hide-region
  "Hides/collapses the given region:
  The content between the cursor and the
  given mark"
  [sl markname]
  (let [d (dirty? sl)
        subsl (get-region-as-slider sl markname)]
    (-> sl
        (delete-region markname)
        (insert-subslider subsl)
        left
        (set-dirty d))))

(defn unhide
  "If current position contains hidden/collapsed
  content it will be expanded."
  [sl]
  (let [d (dirty? sl)
        subsl (first (sl ::after))]
    (if (slider? subsl)
      (-> sl
          right
          (delete 1)
          (insert-slider subsl)
          (set-dirty d))
      sl)))

(defn delete-line
  "Deletes the current line.
  The point will be placed at the beginning
  of the next line."
  [sl]
  (-> sl beginning-of-line
         (set-mark "deleteline")
         end-of-line
         right
         (delete-region "deleteline")))

(defn get-after-list
  [sl]
  (sl ::after))

;; -----------------
;; Derived functions
;; -----------------


(defn swap-line-down
  [sl]
  (if (-> sl end-of-line end?)
    sl
    (let [sl0 (beginning-of-line sl)
          delta (- (get-point sl) (get-point sl0))
          line (-> sl0
                   (set-mark "swap-start")
                   end-of-line
                   (get-region-as-slider "swap-start"))]
      (-> sl0
          (set-mark "swap-start")
          end-of-line
          right
          (delete-region "swap-start")
          end-of-line
          insert-newline
          (insert-slider line)
          beginning-of-line
          (right delta)))))

(defn swap-line-up
  [sl]
  (if (-> sl beginning-of-line beginning?)
    sl
    (let [sl0 (beginning-of-line sl)
          delta (- (get-point sl) (get-point sl0))
          line (-> sl0
                   (set-mark "swap-start")
                   end-of-line
                   (get-region-as-slider "swap-start"))]
      (-> sl0
          (set-mark "swap-start")
          end-of-line
          (delete-region "swap-start")
          (delete 1)
          beginning-of-line
          insert-newline
          left
          (insert-slider line)
          (right delta)))))


;; --------------------------
;; Top of window calculations
;; --------------------------

(defn- left-linebreaks
  "Takes a slider and a number and moves cursor
  back until the nth linebreak.
  So the cursor will be placed right after a linebreak."
  [sl n]
  (nth (iterate #(-> % left (left-until #{"\n"})) sl) n))

(defn- wrap-and-forward-line
  "Goes a visual line forward if the break is soft and insert a hard."
  [sl columns]
  (let [s0 (forward-line sl columns)]
    (cond (= (-> s0 left get-char) "\n") s0
          (end? s0) s0
          (= (get-mark s0 "cursor") (get-point s0)) (-> s0 insert-newline (set-mark "cursor"))
          :else (insert-newline s0))))

(defn update-top-of-window
  "Returns slider where cursor marks has been set
  and point moved to top of window."
  [sl rows columns tow]
  (if (and (beginning? sl) (not= tow 0))
    (update-top-of-window sl rows columns 0)
    (let [sl0 (-> sl (set-mark "cursor") (set-point tow))]
      (if (< (get-mark sl0 "cursor") tow) ;; If point is before top of window
        (let [newtow (get-point (left-linebreaks sl0 (inc rows)))]
          (update-top-of-window sl rows columns newtow))
        (let [;; Add rows number of breaks
              sllist (iterate #(wrap-and-forward-line % columns) sl0)
              slbefore (nth (iterate #(wrap-and-forward-line % columns) sl0) (dec rows))
              sl1 (nth (iterate #(wrap-and-forward-line % columns) sl0) rows)]
    
          ;; If original point is on the first rows of lines we are done
          ;; otherwise a recenter should be performed
          ;(futil/log (str (get-mark sl1 "cursor") ", " (get-point sl1) ", " (pr-str sl1)))
          (if ((if (and (end? sl1) (= (get-point slbefore) (get-point sl1))) <= <) (get-mark sl1 "cursor") (get-point sl1))
            (set-point sl1 tow)
            (let [sl2 (loop [s sl1]
                        (if (<= (get-mark s "cursor") (get-point s))
                          s
                          (recur (wrap-and-forward-line s columns))))
                  ;; Now sl2 ends with the cursor
                  sl3 (right (left-linebreaks sl2 (int (* rows 0.4))) 1)]
              ;; sl3 now has point at new top of window
              sl3
              (update-top-of-window sl rows columns (get-point sl3)))))))))

(defn take-lines
  "Generate list of lines with
  at most columns chars. When exeeding
  end empty lines will be provided."
  [sl rows columns]
  (map #(if (= (get-mark % "beginning") (get-point %))
          ""
          (get-region
            (if (= (get-char (left % 1)) "\n") (left % 1) %) ; The if is to avoid ending with newline
            "beginning"))
    (take rows (rest (iterate #(-> % (set-mark "beginning") (forward-line columns)) sl)))))

(defn find-next-regex
  "Find next regex match. If no match
  found ahead, position will be at end."
  [sl regex]
  (let [s (-> sl after get-content)
        pos (count (first (str/split s regex 2)))]
    (right sl pos)))

(defn find-next
  "Moves the point to the next search match
  from the current point position."
  [sl search]
  (if (not (string? search))
    (find-next-regex sl search)
    (let [s (map str (seq (str/lower-case search)))
          len (count s)]
      (loop [sl0 (right sl 1)]
        (cond (= s (map str/lower-case (take len (sl0 ::after)))) sl0
              (end? sl0) sl
              :else (recur (right sl0 1)))))))

(defn sexp-at-point
  "Returns the sexp at the current point. If there is no
  s-expression nil will be returned."
  [sl]
  (let [sl0 (-> sl (mark-paren-start "paren-start") (mark-paren-end "paren-end"))]
    (if (and (get-mark sl0 "paren-start") (get-mark sl0 "paren-end"))
      (-> sl0 (point-to-mark "paren-start") (get-region "paren-end"))
      nil)))


(defn get-context
  "Calculates a context object like
  a filepath, url, checkbox, code
  collapse and returns the type and
  the matched string.
  Output like {:type :file :value /tmp/tmp.txt}"
  [sl]
  (let [sl0 (-> sl (left-until (partial re-find #"[^ \n\"]"))
                   (left-until (partial re-find #"[ \n\"]"))
                   (right-until (partial re-find #"[\w\.~(\[/$]"))
                   (set-mark "contextstart")) ;right (set-mark "contextstart")) (re-find #"\w" "  x")
        sl1 (-> sl0 (right-until (partial re-find #"[ |\n|\"]")))
        word (str/replace (get-region sl1 "contextstart") #"(\$HOME|~)" (System/getProperty "user.home"))]
    (cond (re-matches #"https?://.*" word) {:type :url :value word}
          (re-matches #";?#" word) {:type :fold :value "fold"}
          (re-matches #"\(.*" word) {:type :function :value (str/replace word #"(\(|\))" "")}
          (re-matches #"[-a-z0-9\.]+/[-a-z0-9]+\)?" word) {:type :function :value (str/replace word #"(\(|\))" "")}
          (re-matches #"/.*" word) {:type :file :value word}
          (re-matches #".*(data:image/png;base64,.*)" word) {:type :base64image :value (re-find #"(?<=base64,)[^\)]*" word)}
          :else {:type :file :value (str/replace word #"[\[\]]" "")})))

