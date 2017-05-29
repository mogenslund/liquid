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
     {::before '()    ; The list of characters before the cursor in reverse order
      ::after after   ; The list of characters after the cursor in normal order
      ::point 0       ; The current point (cursor position). Starts at 0, the begining of the slider
      ::linenumber 1  ; Meta information for fast retrievel of the current line number
      ::totallines (inc (count (filter #(= % "\n") after))) ; Only to make the end function perform optimal
      ::marks {}}))   ; A map of named positions, like "selection" -> 18.
                      ; The positions are pushed, when text is insertet
                      ; strictly before the mark.
  ([] (create "")))

(defn clear
  "Erases everything in the slider,
  content and marks."
  [sl]
  (create))

(defn beginning
  "Moves the point (cursor) to the beginning of the slider."
  [sl]
  (assoc sl
   ::before '()
   ::after (concat (reverse (sl ::before)) (sl ::after))
   ::point 0
   ::linenumber 1))

(defn end
  "Moves the point to the end of the slider."
  [sl]
  (let [tmp (concat (reverse (sl ::after)) (sl ::before))]
    (assoc sl
     ::before tmp
     ::after '()
     ::point (count tmp)
     ::linenumber (sl ::totallines))))


(defn get-char
  "Returns the first character after the point.
  If point is at the end of the slider, nil will
  be returned."
  [sl]
  (first (sl ::after)))

(defn look-behind
  "Returns char behind the cursor given amount back.
  If amount = 1 the char right behind the cursor will be
  returned.
  Non strings will be filtered away.
  If there is no result, nil is returned."
  [sl amount]
  (first (drop (- amount 1) (filter string? (sl ::before)))))

(defn look-ahead
  "Returns char after the cursor given amount forward.
  If amount = 0 the char right after the cursor will be
  returned.
  Non strings will be filtered away.
  If there is no result, nil is returned."
  [sl amount]
  (first (drop amount (filter string? (sl ::after)))))

(defn string-ahead
  "Returns next amount of chars as string.
  Non string will be filtered away."
  [sl amount]
  (str/join "" (take amount (filter string? (sl ::after)))))
  

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
  "Move the point to the left the given amount of times.
  So moving one character left is achieved with
  (left sl 1)."
  [sl amount]
  (if (= amount 1)
    (let [c (first (sl ::before))]
      (cond (nil? c) sl
            (not= c "\n") (assoc sl
                            ::before (rest (sl ::before))
                            ::after (conj (sl ::after) c)
                            ::point (dec (sl ::point)))
            :else (assoc sl
                    ::before (rest (sl ::before))
                    ::after (conj (sl ::after) c)
                    ::point (dec (sl ::point))
                    ::linenumber (dec (sl ::linenumber)))))
    (let [tmp (take amount (sl ::before))             ; Characters to be moved from :before to :after
          n (count tmp)                               ; Might be less than amount, since at most (count :before)
          linecount (count (filter #(= % "\n") tmp))] ; Checking how many lines are moved
      (assoc sl
        ::before (drop n (sl ::before))
        ::after (concat (reverse tmp) (sl ::after))
        ::point (- (sl ::point) n)
        ::linenumber (- (sl ::linenumber) linecount)))))

(defn right-old
  "Move the point to the right the given amount of times."
  [sl amount]
  (let [tmp (take amount (sl ::after))
        n (count tmp)
        linecount (count (filter #(= % "\n") tmp))]
   (assoc sl
    ::before (concat (reverse tmp) (sl ::before))
    ::after (drop n (sl ::after))
    ::point (+ (sl ::point) n)
    ::linenumber (+ (sl ::linenumber) linecount))))

(defn right
  "Move the point to the right the given amount of times."
  [sl amount]
  (if (= amount 1)
    (let [c (first (sl ::after))]
      (cond (nil? c) sl
            (not= c "\n") (assoc sl
                            ::before (conj (sl ::before) c)
                            ::after (rest (sl ::after))
                            ::point (inc (sl ::point)))
            :else (assoc sl
                    ::before (conj (sl ::before) c)
                    ::after (rest (sl ::after))
                    ::point (inc (sl ::point))
                    ::linenumber (inc (sl ::linenumber)))))
    (let [tmp (take amount (sl ::after))
          n (count tmp)
          linecount (count (filter #(= % "\n") tmp))]
      (assoc sl
        ::before (concat (reverse tmp) (sl ::before))
        ::after (drop n (sl ::after))
        ::point (+ (sl ::point) n)
        ::linenumber (+ (sl ::linenumber) linecount)))))

(defn set-point
  "Move point the the given location.
  Not further than beginning of the slider
  and the end of the slider."
  [sl newpoint]
  (if (> newpoint (sl ::point))
    (right sl (- newpoint (sl ::point)))
    (left sl (- (sl ::point) newpoint))))

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
        ::marks (slide-marks (sl ::marks) (+ (sl ::point) n -1) n))))

(defn delete
  "Deletes amount of characters to the left of
  the cursor. So delete 3 of
  aaabbb|ccc wil result in
  aaa|ccc."
  [sl amount]
  (let [tmp (take amount (sl ::before))
        linecount (count (filter #(= % "\n") tmp))
        n (count tmp)]
    (when (= (count (filter list? tmp)) 0) ; Only delete if not hidden
      (assoc sl
        ::before (drop n (sl ::before))
        ::point (- (sl ::point) n)
        ::linenumber (- (sl ::linenumber) linecount)
        ::totallines (- (sl ::totallines) linecount)
        ::marks (slide-marks (sl ::marks) (- (sl ::point) n) n)))))

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

(defn hide
  "Not used yet."
  [sl amount]
  (let [tmp (take amount (sl ::after))

        n (count tmp)]
   (assoc sl
    ::after (conj (drop n (sl ::after)) tmp)
    ::marks (slide-marks (sl ::marks) (- (sl ::point) n) (- n 1)))))

(defn unhide
  "Not used yet."
  [sl]
  )

(defn right-until
  "Moves the cursor forward, until the current char matches the
  regular expression. The cursor will be placed just before the
  character. The function only matches single characters, not
  character sequences!
  If there is no match, the cursor will move all the way to the
  end of the slider.
  Example (cursor = ^):
    aaacde^aacf   -- right-until c -->   aacdeaa^cf."
  [sl regex] ; (re-matches #"(a|b)" "a")
  (loop [s sl]
    (let [c (get-char s)]
      (if (or (end? s) (re-matches regex c))
        s
        (recur (right s 1))))))

(defn left-until
  "Moves the cursor backward, until the current char matches the
  regular expression. The cursor will be places just before the
  character. The function only mathces single characters, not
  character sequences!
  If there is no match, the cursor will move all the way to the
  beginning of the slider.
  Example (cursor = ^):
    aaacde^aacf   -- left-until c -->   aa^cdeaacf."
  [sl regex] ; (re-matches #"(a|b)" "a")
  (loop [s (if (end? sl) (left sl 1) sl)]
    (let [c (get-char s)]
      (if (or (beginning? s) (re-matches regex c))
        s
        (recur (left s 1))))))

(defn mark-paren-start
  "Marks the paren-start of the
  current s-exp. In this case:
  aaa (aaa (aa)  a|aa
  The first paren start is selected."
  [sl]
  (loop [sl0 (-> sl (remove-mark "paren-start") (set-mark "mark-paren-curser")) ch (if (= (get-char sl) ")") "" (get-char sl)) level 0]
    (cond (and (= ch "(") (= level 0)) (-> sl0 (set-mark "paren-start") (point-to-mark "mark-paren-curser"))
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
  [sl]
  (loop [sl0 (-> sl (remove-mark "paren-end") (set-mark "mark-paren-curser")) ch (if (= (get-char sl) "(") "" (get-char sl)) level 0]
    (cond (and (= ch ")") (= level 0)) (-> sl0 (right 1) (set-mark "paren-end") (point-to-mark "mark-paren-curser"))
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
        sl0 (-> sl (mark-paren-start) (mark-paren-end))]
    (if (and (get-mark sl0 "paren-start") (get-mark sl0 "paren-end"))
      (if (= sel (get-mark sl0 "paren-end"))
        (-> sl0 (point-to-mark "paren-start") (set-mark "selection") (point-to-mark "paren-end") (left 1))
        (-> sl0 (point-to-mark "paren-end") (set-mark "selection") (point-to-mark "paren-start")))
      sl)))

(defn highlight-sexp-at-point
  [sl]
  (if (get-mark sl "paren-start")
    (-> sl
      (remove-mark "paren-start")
      (remove-mark "paren-end"))
    (-> sl
      (set-mark "cursor")
      (mark-paren-start)
      (mark-paren-end)
      (point-to-mark "cursor"))))
  

(defn forward-word
  "Move to beginning of next word or end-of-buffer"
  [sl]
  (-> sl (right-until #"\s") (right 1) (right-until #"\S"))) ; (not (not (re-matches #"\S" "\n")))

(defn end-of-line
  "Moves point to the end of the line. Right before the
  next line break."
  [sl]
  (right-until sl #"\n"))

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
    (-> sl (end-of-line) (right 1))))

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
  [sl columns column]
  (let [vc (get-visual-column sl columns)
        sl0 (left sl (+ vc 1))
        vc0 (get-visual-column sl0 columns)]
    (if (> vc0 column)
       (left sl0 (- vc0 column))
       sl0)))

(defn get-region
  "Returns the content between the mark
  with the given name and the point.
  If there is no mark with the given name
  nil is returned."
  [sl markname]
  (let [mark (get-mark sl markname)]
    (cond (nil? mark) nil
          (< mark (get-point sl)) (apply str (reverse
                                             (take (- (get-point sl) mark)
                                                  (sl ::before))))
          :else (apply str (take (- mark (get-point sl)) (sl ::after))))))

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

(defn delete-line
  "Deletes the current line.
  The point will be placed at the beginning
  of the next line."
  [sl]
  (-> sl beginning-of-line
         (set-mark "deleteline")
         end-of-line
         (right 1)
         (delete-region "deleteline")))

(defn get-content
  "The full content of the slider"
  [sl]
  (apply str (-> sl beginning ::after)))

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

(defn find-next
  "Moves the point to the next search match
  from the current point position."
  [sl search]
  (let [s (map str (seq (str/lower-case search)))
        len (count s)]
    (loop [sl0 (right sl 1)]
      (cond (= s (map str/lower-case (take len (sl0 ::after)))) sl0
            (end? sl0) sl
            :else (recur (right sl0 1))))))


;(defn frame
;  [sl rows columns top-of-window]
;  (if (< (get-point sl) top-of-window)
;    (frame sl rows columns 0)
;    (let [point0 (get-point sl)
;          ;; A lazy list of sliders, each with point one line ahead of the preceeding
;          linesliders (iterate
;                        #(-> % (set-mark "beginning") (forward-line columns))
;                        (-> sl (set-point top-of-window) (set-mark "beginning")))
;          ;; Number of rows to go, to get to point
;          pointrow (+ (count (take-while
;                             #(< (get-point %) point0)
;                             linesliders))
;                      (if (or (= (get-char (left sl 1)) "\n") (beginning? sl)) 1 0))
;          pointcol (- point0 (get-mark (nth linesliders pointrow) "beginning") -1)] 
;      (if (> pointrow rows)
;          ;; Recenter
;          (frame sl rows columns (get-point (nth linesliders (- pointrow (int (* 0.4 rows))))))
;          ;; Generate lines from marked list of sliders
;          ;; todo: Check performance by replace map with pmap
;          (let [lines (map #(if (= (get-mark % "beginning") (get-point %))
;                                ""
;                                (get-region
;                                  (if (= (get-char (left % 1)) "\n") (left % 1) %) ; The if is to avoid ending with newline
;                                  "beginning"))
;                           (take rows (rest linesliders)))]
;            {:lines lines
;             :top-of-window top-of-window
;             :cursor {:row pointrow :column pointcol}
;             :selection {:column nil :row nil}})))))