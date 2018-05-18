(ns dk.salza.liq.buffer
  "A buffer could be considered a slider (see slider) with
  some extra attributes, besides attributes to keep track
  of name, filename, dirtyness, undo information it consists
  of

    * A slider
    * A highlighter function
    * A keymap

  The slider is used to keep track of the text, the highlighter
  function is used by the view to apply highlight and the
  keymap is used to decide which function a keypress should
  be mapped to.

  On runtime the highlighter function and the keymap can be
  replaced.
  
  The buffer structure is immutable, so every operation that
  changes the buffer will return a new one."
  (:require [dk.salza.liq.slider :as slider]
            [dk.salza.liq.tools.fileutil :as fileutil]
            [dk.salza.liq.coreutil :refer :all]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn create
  "Creates an empty buffer with the given
  name."
  [name]
  {::name name
   ::slider (slider/create)
   ::slider-undo '()  ;; Conj slider into this when doing changes
   ::slider-stack '() ;; To use in connection with undo
   ::filename nil
   ::modified nil
   ::dirty false
   ::mem-col 0
   ::highlighter nil
   ::keymap {}
   })

(defn create-slider-from-file
  [path]
  (if (and (.exists (io/file path)) (.isFile (io/file path)))
    (let [r (io/reader path)]
      (loop [c (.read r) sl (slider/create)]
        (if (not= c -1)
          (recur (.read r) (slider/insert sl (str (char c))))
          (slider/beginning sl))))
    (slider/create)))

(defn create-from-file
  "Creates a buffer and loads the content of a given file.
  The filename is stored also, to be used for save
  functionality."
  [path]
  {::name path
   ::slider (create-slider-from-file path)
   ::slider-undo '()  ;; Conj slider into this when doing changes
   ::slider-stack '() ;; To use in connection with undo
   ::filename path
   ::modified (.lastModified (io/file path))
   ::dirty false
   ::mem-col 0
   ::highlighter nil
   ::keymap {}})

(defn- doto-slider
  [buffer fun & args]
  (update-in buffer [::slider] #(apply fun (list* % args))))

(defn apply-to-slider
  "Apply function to the slider in the buffer.
  It should take a slider as input and produce a slider
  as output."
  [buffer fun]
  (update-in buffer [::slider] fun))

(defn update-mem-col
  "The mem-col is the column position of the cursor,
  from the last time the cursor was moved not up and
  down.

  When moving the cursor up and down and passing by
  shorter lines the column position, when reaching a
  longer line again, should be restored.
  
  This allows one to keep the current column and
  avoid the cursor from staying at the beginning of
  the line when passing an empty line."
  [buffer columns]
  (assoc buffer ::mem-col (slider/get-visual-column (buffer ::slider) columns)))

(defn changed-on-disk?
  ""
  [buffer]
  (if (buffer ::modified)
    (not= (buffer ::modified) (.lastModified (io/file (buffer ::filename))))
    false))

(defn update-modified
  ""
  [buffer]
  (if-let [filepath (buffer ::filename)]
    (assoc buffer ::modified (.lastModified (io/file filepath)))
    buffer))

(defn get-slider
  "Returns the slider in the buffer datastructure.
  The slider is responsible for storing and manipulating
  the text in the buffer."
  [buffer]
  (buffer ::slider))

(defn set-slider
  [buffer sl]
  (assoc buffer ::slider sl))

(defn get-visible-content
  "Not in use yet, since there is no functionality
  for hiding lines, yet."
  [buffer]
  (-> buffer ::slider slider/get-visible-content)) 

(defn- set-undo-point
  "Return new buffer with the current slider to the undo stack."
  [buffer]
  (let [newstack (conj (buffer ::slider-stack) (buffer ::slider))]
    (assoc buffer ::slider-stack newstack
                  ::slider-undo newstack)))

(defn undo
  "Returns the first buffer in the undo stack."
  [buffer]
  (if (empty? (buffer ::slider-undo))
    buffer
    (assoc buffer ::slider (-> buffer ::slider-undo first)
                  ::slider-stack (conj (buffer ::slider-stack) (-> buffer ::slider-undo first))
                  ::slider-undo (rest (buffer ::slider-undo)))))

(defn set-highlighter
  "Returns a new buffer with the given highlighter set."
  [buffer highlighter]
  (assoc buffer ::highlighter highlighter))

(defn get-highlighter
  "Returns the highlighter function associated
  with the buffer."
  [buffer]
  (buffer ::highlighter))

(defn set-keymap
  "Returns a new buffer with the given
  keymap set."
  [buffer keymap]
  (assoc buffer ::keymap keymap))

(defn get-keymap
  "Returns the keymap associated with the buffer."
  [buffer]
  (buffer ::keymap))

(defn set-dirty
  "Sets dirty flag on the buffer.
  Used to mark if content has changed
  since last save."
  ([buffer dirty]
    (if (buffer ::filename) (assoc buffer ::dirty dirty) buffer))
  ([buffer]
    (set-dirty buffer true)))

(defn get-dirty
  "Returns the dirty state of the buffer."
  [buffer]
  (buffer ::dirty))

(defn get-filename
  "Returns the filename associated with the buffer."
  [buffer]
  (buffer ::filename))

(defn get-name
  "Returns the name associated with the buffer."
  [buffer]
  (buffer ::name))

(defn forward-char
  "Returns a new buffer where the cursor has been
  moved amount steps forward."
  [buffer amount]
  (doto-slider buffer slider/right amount))

(defn backward-char
  "Returns a new buffer where the cursor has been
  moved amount steps backward."
  [buffer amount]
  (doto-slider buffer slider/left amount))

(defn forward-word
  "Returns a new buffer with the cursor moved past
  next whitespace."
  [buffer]
  (doto-slider buffer #(-> % (slider/right-until (partial re-find #"\s")) (slider/right-until (partial re-find #"\S")))))

(defn backward-word
  "Returns a new buffer with the cursor moved backward
  to the beginning of the previous word."
  [buffer]
  (doto-slider buffer #(let [new-buffer (-> % (slider/left 1) (slider/left-until (partial re-find #"\S")) (slider/left-until (partial re-find #"\s")))] 
                        (if (slider/beginning? new-buffer) new-buffer (slider/right new-buffer)))))

(defn end-of-word
  "Returns a new buffer with the cursor moved
  to the end of the current word."
  [buffer]
  (doto-slider buffer #(-> % (slider/right 1) (slider/right-until (partial re-find #"\S")) (slider/right-until (partial re-find #"\s")) (slider/left 1))))

(defn beginning-of-buffer
  "Returns a new buffer where the cursor is
  moved to the beginning of the buffer."
  [buffer]
  (doto-slider buffer slider/beginning))

(defn end-of-buffer
  "Returns a new buffer where the cursor is
  moved to the end of the buffer."
  [buffer]
  (doto-slider buffer slider/end))

(defn find-next
  "Returns a new buffer where the cursor is moved
  to the next occurrence of the search frase."
  [buffer search]
  (doto-slider buffer slider/find-next search))

(defn insert
  "Returns a new buffer with the given string
  inserted a the cursor position."
  [buffer string]
  (set-dirty (doto-slider (set-undo-point buffer) slider/insert string)))

(defn delete
  "Returns a new buffer with the given amount of
  characters deleted behind the cursor."
  [buffer amount]
  (set-dirty (doto-slider (set-undo-point buffer) slider/delete amount)))

(defn set-mark
  "Returns a new buffer with a mark set at the cursor position."
  [buffer name]
  (doto-slider buffer slider/set-mark name))

(defn set-point
  "Returns a new buffer where the cursor has been
  moved to the given point."
  [buffer point]
  (doto-slider buffer slider/set-point point))

(defn remove-mark
  "Returns a new buffer with the named mark removed."
  [buffer name]
  (doto-slider buffer slider/remove-mark name))

(defn point-to-mark
  "Returns a new buffer where the cursor has been
  moved to the given mark."
  [buffer name]
  (doto-slider buffer slider/point-to-mark name))

(defn end-of-line
  "Returns a new buffer where the cursor has been
  moved to the end of the line."
  [buffer]
  (doto-slider buffer slider/end-of-line))

(defn beginning-of-line
  "Returns a new buffer where the cursor has been
  moved to the beginning of the line."
  [buffer]
  (doto-slider buffer slider/beginning-of-line))

(defn insert-line
  "Returns a new buffer where a new line has been
  inserted below the current line, and the cursor
  has been moved to that line."
  [buffer]
  (set-dirty (doto-slider (set-undo-point buffer) #(-> % slider/end-of-line (slider/insert "\n")))))

(defn delete-char
  "Returns a new buffer where the character after
  the curser is removed."
  [buffer]
  (if (slider/end? (buffer ::slider))
    buffer
    (set-dirty (doto-slider (set-undo-point buffer) #(-> % (slider/right 1) (slider/delete 1))))))

(defn clear
  "Returns a new buffer where all the content has
  been deleted."
  [buffer]
  (set-dirty (doto-slider (set-undo-point buffer) slider/clear)))

(defn get-context
  "Returns a context map like
  {:type :file :value /tmp/tmp.txt}
  with a type and a value generated by analysing
  the context of the cursor."
  [buffer]
  (slider/get-context (get-slider buffer)))

(defn delete-selection
  "Returns a buffer where the selected content
  is deleted."
  [buffer]
  (doto-slider buffer slider/delete-region "selection"))

(defn delete-line
  "Returns a buffer where the current lines is deleted
  and the cursor is moved to the next line."
  [buffer]
  (doto-slider buffer slider/delete-line))

(defn select-sexp-at-point
  "Returns a buffer where the s-expression at the point (cursor)
  has been selected.
  The cursor is moved backwards until the first
  startparenthesis where selection starts, and then
  forward until the selection has balanced parenthesis."
  [buffer]
  (doto-slider buffer slider/select-sexp-at-point))

(defn highlight-sexp-at-point
  "Returns a buffer where the highlight marks
  have been set matching the current s-expression.
  It spans same text as select-sexp-at-point."
  [buffer]
  (doto-slider buffer slider/highlight-sexp-at-point))

(defn forward-visual-line
  "Returns a buffer where the cursor has been moved
  forward one visual (soft) line, taking wrap at given
  columns into account."
  [buffer columns]
  (doto-slider buffer slider/forward-visual-column columns (buffer ::mem-col)))

(defn backward-visual-line
  "Returns a buffer where the cursor has been moved
  backward one visual (soft) line, taking wrap at given
  columns into account."
  [buffer columns]
  (doto-slider buffer slider/backward-visual-column columns (buffer ::mem-col)))

(defn get-region
  "Returns the region between the cursor and the
  named mark, as a string."
  [buffer name]
  (slider/get-region (buffer ::slider) name))

(defn get-char
  "Returns the current char at point as a string."
  [buffer]
  (slider/get-char (buffer ::slider)))

(defn get-point
  "Returns the point (cursor position) as a number."
  [buffer]
  (slider/get-point (buffer ::slider)))

(defn get-linenumber
  "Returns the line number at the cursor position."
  [buffer]
  (slider/get-linenumber (buffer ::slider)))

(defn get-mark
  "Returns the position af the given mark."
  [buffer name]
  (slider/get-mark (buffer ::slider) name))

(defn get-selection
  "Returns the selection region as a string."
  [buffer]
  (-> buffer (get-region "selection"))) ;(slider/get-region (buffer ::slider) "selection"))

(defn get-content
  "Returns the content (text) of the buffer
  as a string."
  [buffer]
  (slider/get-content (buffer ::slider)))

(defn sexp-at-point
  "Returns the s-expression at the cursor position."
  [buffer]
  (slider/sexp-at-point (buffer ::slider)))

(defn get-line
  "Returns the current line as a string."
  [buffer]
  (-> buffer beginning-of-line (set-mark "linestart")
             end-of-line (get-region "linestart")))

(defn get-action
  "If the keymap has an action (function)
  for the given keyword, it will be returned."
  [buffer keyw]
  (when (buffer ::keymap)
    ((buffer ::keymap) keyw)))

(defn end-of-buffer?
  "Return true if the cursor is at the
  end of the buffer."
  [buffer]
  (slider/end? (buffer ::slider)))

(defn save-buffer
  "If there is a filename connected with the buffer,
  the content of the buffer will be saved to that file."
  [buffer]
  (when-let [filepath (get-filename buffer)]
    (fileutil/write-file filepath (get-content buffer))
    (assoc buffer ::modified (.lastModified (io/file filepath))))
  (-> buffer
    (set-dirty false)
    update-modified))
  
(defn force-reopen-file
  "Reopening file in buffer,
  ignore dirty flag."
  [buffer]
  (if-let [filepath (get-filename buffer)]
    (let [sl (create-slider-from-file filepath)
          p (get-point buffer)]
      (-> buffer
        (set-slider sl)      
        (set-dirty false)
        update-modified
        (set-point p)))
    buffer))

(defn reopen-file
  "Reopen file in buffer,
  if the file is not dirty."
  [buffer]
  (if (get-dirty buffer)
    buffer
    (force-reopen-file buffer)))


(defn tmp-buffer
  [buffer columns]
  (doto-slider buffer slider/forward-line columns))

