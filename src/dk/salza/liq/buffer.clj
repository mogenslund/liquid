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
   ::slider-undo ()  ;; Conj slider into this when doing changes
   ::slider-stack () ;; To use in connection with undo
   ::filename nil
   ::modified nil
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
          (slider/set-dirty (slider/beginning sl) false))))
    (slider/create)))

(defn create-from-file
  "Creates a buffer and loads the content of a given file.
  The filename is stored also, to be used for save
  functionality."
  [path]
  {::name path
   ::slider (create-slider-from-file path)
   ::slider-undo ()  ;; Conj slider into this when doing changes
   ::slider-stack () ;; To use in connection with undo
   ::filename path
   ::modified (.lastModified (io/file path))
   ::mem-col 0
   ::highlighter nil
   ::keymap {}})

(defn- doto-slider
  [buffer fun & args]
  (update buffer ::slider #(apply fun (list* % args))))

(defn apply-to-slider
  "Apply function to the slider in the buffer.
  It should take a slider as input and produce a slider
  as output."
  [buffer fun]
  (update buffer ::slider fun))

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

(defn set-undo-point
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
    (doto-slider buffer slider/set-dirty dirty))
  ([buffer]
    (doto-slider buffer slider/set-dirty true)))

(defn dirty?
  "Returns the dirty state of the buffer."
  [buffer]
  (when (buffer ::filename) (-> buffer get-slider slider/dirty?)))

(defn get-filename
  "Returns the filename associated with the buffer."
  [buffer]
  (buffer ::filename))

(defn get-name
  "Returns the name associated with the buffer."
  [buffer]
  (buffer ::name))

(defn find-next
  "Returns a new buffer where the cursor is moved
  to the next occurrence of the search frase."
  [buffer search]
  (doto-slider buffer slider/find-next search))

(defn set-point
  "Returns a new buffer where the cursor has been
  moved to the given point."
  [buffer point]
  (doto-slider buffer slider/set-point point))

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

(defn get-action
  "If the keymap has an action (function)
  for the given keyword, it will be returned."
  [buffer keyw]
  (when (buffer ::keymap)
    ((buffer ::keymap) keyw)))

(defn save-buffer
  "If there is a filename connected with the buffer,
  the content of the buffer will be saved to that file."
  [buffer]
  (when-let [filepath (get-filename buffer)]
    (fileutil/write-file filepath (-> (get-slider buffer) slider/get-content))
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
          p (-> (get-slider) slider/get-point)]
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
  (if (dirty? buffer)
    buffer
    (force-reopen-file buffer)))


(defn tmp-buffer
  [buffer columns]
  (doto-slider buffer slider/forward-line columns))

