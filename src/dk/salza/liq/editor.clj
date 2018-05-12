(ns dk.salza.liq.editor
  "The editor is the central point of Liquid.
  Most of the data is immutable, but the editor has
  a ref, also called editor. It contains the state of
  the editor.

  Most actions on the editor will replace content of this state
  with a transformed content.

  For example:

    When (forward-char 1) is called, the current buffer will be
    replaced with a new buffer, where the cursor is moved one char
    ahead."
  (:require [dk.salza.liq.buffer :as buffer]
            [dk.salza.liq.slider :as slider]
            [dk.salza.liq.window :as window]
            [dk.salza.liq.tools.util :as util]
            [dk.salza.liq.clojureutil :as clojureutil]
            [clojure.java.io :as io]
            [dk.salza.liq.coreutil :refer :all]
            [dk.salza.liq.logging :as logging]
            [dk.salza.liq.tools.cshell :as cshell]
            [clojure.string :as str]))

(def top-of-window
  "Atom to keep track of the position
  of the first char visible in a window.
  Keys are generated from window and buffer
  names."
  (atom {}))

(defn get-top-of-window
  [keyw]
  (@top-of-window keyw))

(defn set-top-of-window
  [keyw val]
  (when (not= (get-top-of-window keyw) val)
    (swap! top-of-window assoc keyw val)))

(def ^:private macro-seq (atom '())) ; Macrofunctionality might belong to input handler.
(def ^:private macro-record (atom false))
(def ^:private submap (atom nil))
(def ^:private tmpmap (atom {})) ; Keymap for shortterm keybindings
(def ^:private keylist (atom '()))


(def updates
  "Variable to be increased when updates
  are done to the editor, to allow easy
  check if redraw is needed."
  (atom 0))

(def ^:private fullupdate (atom false))


(def ^:private empty-editor
  {::buffers '()
   ::windows '()
   ::global-keymap {}
   ::file-eval {}
   ::frame-dimensions {::rows 40 ::columns 140}
   ::settings {::default-keymap nil
               ::key-info false
               ::default-highlighter nil
               ::default-app nil
               ::searchstring ""
               ::searchpaths '()
               ::rootfolders '()
               ::files '()
               ::snippets '()
               ::commands '()
               ::interactive '()}})

(def editor
  "The ref which contains the editor data, that is
  
  * List of buffers
  * List of windows
  * Global keymap
  * Map of functions to evaluate different filetypes
  * Settings with searchpaths, searchfiles, snippets,
    commands and interactive commands for typeahead.

  Most functions in the editor namespace manipulates
  this ref."
  (atom empty-editor))

(defn reset
  "Resets the editor. Mostly for testing purposes."
  []
  (reset! editor empty-editor))

(defn updated
  "Call this function to proclaim that an
  update has been made to the editor.
  This can be used by views to check for updates."
  []
  (swap! updates inc)
  nil)

(defn get-key-list
  "List of all typed keys"
  []
  @keylist)

(defn fullupdate?
  []
  (when @fullupdate
    (reset! fullupdate false)
    true))

(defn request-fullupdate
  []
  (reset! fullupdate true))

(defn setting
  "Get the setting with the given key.

  Settings are used as a key value store
  in the editor.

  Items like snippets or search paths are
  store in this."
  [keyw]
  (-> @editor ::settings keyw))

(defn add-to-setting
  "When a setting with a given key is a list,
  items can be added to that list using this
  function."
  [keyw entry]
  (when (not (some #{entry} (setting keyw)))
    (swap! editor update-in [::settings keyw] conj entry)))

(defn set-setting
  "Set keyw to value in the
  settings part of the editor." 
  [keyw value]
  (swap! editor assoc-in [::settings keyw] value))

(defn set-default-keymap
  "Set the keymap to be used as default when a
  new buffer is created."
  [keymap]
  (set-setting ::default-keymap keymap))

(defn set-tmp-keymap
  [keymap]
  (reset! tmpmap keymap))

(defn drop-tmp-keymap
  []
  (set-tmp-keymap {}))

(defn set-default-highlighter
  "Set the highlighter function to be used as
  default when a new buffer is created."
  [highlighter]
  (set-setting ::default-highlighter highlighter))

(defn get-default-highlighter
  "Get the default highlighter function.
  Mostly used to set highlight on buffer
  until some is set by user or app."
  []
  (setting ::default-highlighter))

(defn set-default-typeahead-function
  [typeaheadfn]
  (set-setting ::default-typeahead typeaheadfn))

(defn get-default-typeahead-function
  []
  (setting ::default-typeahead))

(defn set-default-app
  "Set the default app to be used when a new
  buffer is created."
  [app]
  (set-setting ::default-app app))

(defn get-default-app
  []
  (setting ::default-app))

(defn get-frame-rows
  []
  (-> @editor ::frame-dimensions ::rows))

(defn get-frame-columns
  []
  (-> @editor ::frame-dimensions ::columns))

(defn- doto-buffer
  "Apply the given function to the top-most buffer."
  [fun & args]
  (swap! editor update ::buffers #(apply doto-first (list* % fun args)))
  nil)

(defn- doto-windows
  "Apply the given function to the windowlist."
  [fun & args]
  (swap! editor update ::windows #(apply fun (list* % args)))
  (request-fullupdate)
  (updated)
  nil)

(defn- doto-window
  "Apply the given function to the current."
  [fun & args]
  (swap! editor update ::windows #(apply doto-first (list* % fun args)))
  (updated)
  nil)

(defn current-buffer
  "Returns the current active buffer.
  Since buffers are immutable, this will
  be a copy of the actual buffer."
  []
  (-> @editor ::buffers first))

(defn current-window
  "Get the current active window.
  Mostly used to get the dimensions
  for the related buffer."
  []
  (-> @editor ::windows first))

(defn set-global-key
  "Define a global keybinding.
  It takes a keyword like :f5 and a function
  to call when that key is pressed."
  ([keyw fun]
   (swap! editor assoc-in [::global-keymap keyw] fun) nil)
  ([keyw1 keyw2 fun]
     (when (not ((@editor ::global-keymap) keyw1))
       (swap! editor assoc-in [::global-keymap keyw1] {}))
     (swap! editor assoc-in [::global-keymap keyw1 keyw2] fun) nil))

(defn set-eval-function
  "Associate an extension with a function. The function
  is assumed to take one input - the filepath.
  EXAMPLE (To associate files with \"py\" extension with the
           python command):
    (editor/set-eval-function \"py\" #(cshell/cmd \"python\" %))"
  [extension fun]
  (swap! editor assoc-in [::file-eval extension] fun) nil)
(defn add-command
  "Add a command to be availble for commandapp typeahead.
  add-interactive is in most cases more suitable."
  [fun]
  (add-to-setting ::commands fun) nil)

(defn add-searchpath
  "Add a folder to searchpaths.
  When using the commandapp files below
  folders in the searchpath will be available
  through typeahead.
  EXAMPLE: (editor/add-searchpath \"/tmp\")"
  [s]
  (add-to-setting ::searchpaths s) nil)

(defn get-searchpaths
  []
  (setting ::searchpaths))

(defn add-rootfolder
  "Add a folder to rootfolders.
  When using find file app, the rootfolders
  can be directly accessed through typeahead.
  EXAMPLE: (editor/add-rootfolder \"/tmp\")"
  [s]
  (add-to-setting ::rootfolders s) nil)

(defn get-rootfolders
  []
  (setting ::rootfolders))

(defn add-snippet
  "Add a snippet to list of snippets.
  They will be available through typeahead
  from the commandapp.
  When chosen the snippet text will be inserted.
  EXAMPLE: (editor/add-snippet \"(ns user)\")"
  [s]
  (add-to-setting ::snippets s) nil)

(defn add-file
  "Add a single file to be availalbe through typeahead
  from the commandapp. When chosen the file will
  be opened.
  EXAMPLE: (editor/add-file \"/home/mogens/.liq\")"
  [f]
  (add-to-setting ::files f) nil)

(defn add-interactive
  "Add an interactive function.
  The label will be shown in typeahead.
  The user will be prompted to provide and
  input for each of the lables in arglabels.
  The function will be called with the input
  values as arguments."
  [label fun & arglabels]
  (add-to-setting ::interactive [label fun arglabels])
  nil)

(defn add-window
  "Add a window to the editor.
  It takes as input a window created using
  dk.salza.liq.window/create function."
  ([window]
   (swap! editor update ::windows conj window))
  ([name top left rows columns buffername]
   (add-window (window/create name top left rows columns buffername))))

(defn get-windows
  "Returns the list of windows in the editor.
  Used by views to get the dimensions of the
  visible buffers."
  []
  (@editor ::windows))

(defn get-buffer
  "Get a buffer by its name.
  Since buffers are immutable, the buffer given
  will be a copy of the buffer."
  [name]
  (first (filter #(= (% ::buffer/name) name) (@editor ::buffers))))

(defn buffer-names
  "The names of the buffers as a list"
  []
  (map ::buffer/name (@editor ::buffers)))

(defn dirty-buffers
  "The names of the dirty buffers as a list."
  []
  (map ::buffer/name (filter ::buffer/dirty (@editor ::buffers))))

(defn switch-to-buffer
  "Switch to the buffer with the given name."
  [buffername]
  (swap! editor update ::buffers bump ::buffer/name buffername)
    (let [win (get-match (get-windows) ::window/buffername buffername)]
      (if win
        (swap! editor update ::windows bump ::window/buffername buffername)
        (swap! editor update ::windows doto-first assoc ::window/buffername buffername))))

(defn update-mem-col
  "Stores the current cursor position on the current line.
  Primarily used for forward-line and backward-line to
  remember the cursor position when navigation is done
  past shorter lines.
  That is what makes the cursor in and out when using
  arrow down."
  []
  (doto-buffer buffer/update-mem-col
               ((current-window) ::window/columns)))

(defn apply-to-slider
  "Apply function to the slider in the current buffer.
  It should take a slider as input and produce a slider
  as output."
  [fun]
  (doto-buffer buffer/apply-to-slider fun)
  (update-mem-col))

(defn get-slider
  []
  (-> (current-buffer) buffer/get-slider))

(defn set-slider
  [sl]
  (doto-buffer buffer/set-slider sl)
  (update-mem-col))

(defn get-filename
  "The filename if one is associated with the current
  buffer, otherwise nil."
  []
  (-> (current-buffer) buffer/get-filename))

(defn get-folder
  "The folder part if a filename is associated with
  the current buffer, otherwise nil."
  []
  (if-let [filepath (get-filename)]
    (str (.getParent (io/file filepath)))))

(defn- get-visible-content
  "Not in use yet, since there is no functionality
  for hiding lines, yet."
  []
  (-> (current-buffer) buffer/get-visible-content))

(defn insert
  "Inserts a string to the current active buffer
  at the cursor position."
  [string]
  (doto-buffer buffer/insert string) (update-mem-col))

(defn insert-line
  "Inserts an empty line below the current
  and move the cursor down."
  []
  (doto-buffer buffer/insert-line)
  (update-mem-col))

(defn forward-char
  "Moves the cursor forward the given amount,
  or 1 step, if no arguments are given."
  ([amount]
    (doto-buffer buffer/forward-char amount)
    (update-mem-col))
  ([]
    (doto-buffer buffer/forward-char 1)
    (update-mem-col)))

(defn forward-word
  "Moves the cursor to the beginning
  of the next word."
  []
  (doto-buffer buffer/forward-word)
  (update-mem-col))

(defn end-of-word
  "Moves the cursor to the end of the current
  word."
  []
  (doto-buffer buffer/end-of-word)
  (update-mem-col))

(defn backward-char
  "Moves the cursor backward the given amount,
  or 1 step, if no arguments are given."
  ([amount]
    (doto-buffer buffer/backward-char amount)
    (update-mem-col))
  ([]
    (doto-buffer buffer/backward-char 1)
    (update-mem-col)))

(defn delete
  "Deletes given amount of characters backwards.
  If no amount is supplied just one character
  will be deleted."
  ([amount]
    (doto-buffer buffer/delete amount)
    (update-mem-col))
  ([]
    (doto-buffer buffer/delete 1)
    (update-mem-col)))

(defn delete-char
  "Deletes the character after the cursor."
  []
  (doto-buffer buffer/delete-char)
  (update-mem-col))

(defn replace-char
  "Replaces the char at current point
  with the given one."
  [s]
  (delete-char)
  (insert s)
  (backward-char))

(defn end-of-line
  "Moves the cursor to the end of the
  current line. The next hard line break."
  []
  (doto-buffer buffer/end-of-line)
  (update-mem-col))

(defn beginning-of-line
  "Moves the cursor to the beginning
  of the current line."
  []
  (doto-buffer buffer/beginning-of-line)
  (update-mem-col))

(defn beginning-of-buffer
  "Moves the cursor to the beginning of the buffer."
  []
  (doto-buffer buffer/beginning-of-buffer)
  (update-mem-col))

(defn end-of-buffer
  "Moved the cursor to the end of the buffer."
  []
  (doto-buffer buffer/end-of-buffer)
  (update-mem-col))

(defn clear
  "Clears the whole buffer."
  []
  (doto-buffer buffer/clear))

(defn selection-active?
  []
  (buffer/get-mark (current-buffer) "selection"))

(defn selection-set
  "Sets selection mark at the current point.
  This will be the starting point of a selection.
  Cursor position will be the end point of the
  selection."
  []
  (doto-buffer buffer/set-mark "selection"))

(defn selection-cancel
  "Removes the selection point.
  Nothing will be selected afterwards."
  []
  (doto-buffer buffer/remove-mark "selection"))

(defn selection-toggle
  "If something is selected, the selection
  will be cancelled.
  If nothing is selected a selection
  will be initiated."
  []
  (if (buffer/get-mark (current-buffer) "selection")
    (selection-cancel)
    (selection-set)))

(defn select-sexp-at-point
  "Selects the s-expression at the point (cursor)
  The cursor is moved backwards until the first
  startparenthesis where selection starts, and then
  forward until the selection has balanced parenthesis."
  []
  (doto-buffer buffer/select-sexp-at-point))

(defn highlight-sexp-at-point
  "Like select-sexp-at-point but only highlights
  start and end of s-expression.
  Toggle this to see current s-expression."
  []
  (doto-buffer buffer/highlight-sexp-at-point))

(defn undo
  "Execute an undo on the current buffer."
  []
  (doto-buffer buffer/undo))

(defn get-selection
  "Get current selected text as a string.
  Returns nil if nothing is selected."
  []
  (-> (current-buffer) buffer/get-selection))

(defn get-content
  "Return the content of the current buffer
  as a string."
  []
  (-> (current-buffer) buffer/get-content))

(defn sexp-at-point
  "Returns the s-expression at the cursor position."
  []
  (-> (current-buffer) buffer/sexp-at-point))

(defn get-context
  "Returns a context map like
  {:type :file :value /tmp/tmp.txt}
  with a type and a value generated by analysing
  the context of the cursor."
  []
  (-> (current-buffer) buffer/get-context))

(defn get-line
  "Returns the current line as a string."
  []
  (-> (current-buffer) buffer/get-line))

(defn get-char
  "Return the char at the point as a string."
  []
  (-> (current-buffer) buffer/get-char))

(defn get-name
  "Returns the name of the current buffer."
  []
  (-> (current-buffer) buffer/get-name))

(defn get-point
  "Returns the point as a number."
  []
  (-> (current-buffer) buffer/get-point))

(defn set-mark
  "Sets a named mark at the current point
  on the current buffer"
  [name]
  (doto-buffer buffer/set-mark name))

(defn get-mark
  "Returns the named mark on the current buffer
  as a number, the position of the mark."
  [name]
  (-> (current-buffer) (buffer/get-mark name)))

(defn remove-mark
  "Removes the named mark from the current buffer."
  [name]
  (doto-buffer buffer/remove-mark name))

(defn point-to-mark
  "Move the point to the mark on the current buffer."
  [name]
  (doto-buffer buffer/point-to-mark name))

(defn end-of-buffer?
  "Returns true of the current point is the
  end of the current buffer."
  []
  (-> (current-buffer) buffer/end-of-buffer?))

(defn forward-line
  "Moves cursor forward one line
  in the current active buffer."
  []
  (doto-buffer buffer/forward-visual-line ((current-window) ::window/columns)))

(defn backward-line
  "Moves cursor backward one line
  in the current active buffer."
  []
  (doto-buffer buffer/backward-visual-line ((current-window) ::window/columns)))

(defn forward-page
  "Moves one page forward on the current buffer.
  A page is what is visible in the current window."
  []
  (let [towid (window/get-towid (current-window) (current-buffer))]
    (doto-buffer buffer/set-point (or (get-top-of-window towid)  0))
    (dotimes [n ((current-window) ::window/rows)] (forward-line))
    (beginning-of-line)
    (set-top-of-window towid (get-point))))

(defn top-align-page
  "Scrolls so the current line is at the top
  of the window."
  []
  (let [towid (window/get-towid (current-window) (current-buffer))]
    (beginning-of-line)
    (set-top-of-window towid (get-point))))

(defn other-window
  "Navigates to the next window and changes
  buffer accordingly."
  []
  (let [buffername (-> @editor ::windows second (window/get-buffername))]
    (swap! editor update ::windows rotate)
    (swap! editor update ::buffers bump ::buffer/name buffername)))

(defn enlarge-window-right
  [amount] 
   (doto-windows window/enlarge-window-right amount))

(defn shrink-window-right
  [amount] 
   (doto-windows window/shrink-window-right amount))

(defn enlarge-window-below
  [amount] 
  (doto-windows window/enlarge-window-below amount))

(defn shrink-window-below
  [amount] 
  (doto-windows window/shrink-window-below amount))

(defn split-window-right
  ([amount] 
   (doto-windows window/split-window-right amount))
  ([]
   (split-window-right 0.5)))

(defn split-window-below
  ([amount] 
   (doto-windows window/split-window-below amount))
  ([]
   (split-window-below 0.5)))

(defn delete-window
  []
  (doto-windows window/delete-window)
  (switch-to-buffer (-> (get-windows) first window/get-buffername)))

(defn previous-buffer
  "Navigates to the previous buffer used."
  []
  (switch-to-buffer (-> @editor ::buffers second ::buffer/name)))

(defn previous-real-buffer
  "Navigates to the prevous buffer, but skip over
  buffer with names containing dashes."
  []
  (when-let [previous (first (filter (fn [x] (not (re-find #"^-" x))) (rest (buffer-names))))]
    (switch-to-buffer previous)))

(defn set-keymap
  "Sets the keymap on the current buffer."
  [keymap]
  (doto-buffer buffer/set-keymap keymap))

(defn get-keymap
  "Returns the active keymap on from the current buffer.
  This is used by the editor to determine what action
  to take."
  []
  (buffer/get-keymap (current-buffer)))

(defn set-highlighter
  "Set the highlighter function on the current
  buffer. This determines how the content
  in the buffer is syntax highlighted."
  [highlighter]
  (doto-buffer buffer/set-highlighter highlighter))

(defn get-highlighter
  "Returns the highlighter function from
  the current buffer."
  []
  (buffer/get-highlighter (current-buffer)))

(defn get-action
  "If the current buffer has an action for the given
  keyword the action (function) is return, otherwise,
  if there is a global action for the given keyword
  that will be returned."
  [keyw]
  (or (@tmpmap keyw)
      (buffer/get-action (current-buffer) keyw)
      ((@editor ::global-keymap) keyw)))

(defn new-buffer
  "Create a new buffer with the given name.
  The buffer will be set as the current buffer
  in the current window.
  Default highlighter and keymaps are assign.
  They can be overridden afterwards."
  [name]
  (when (not (get-buffer name))
    (swap! editor update ::buffers conj (buffer/create name)))
  (switch-to-buffer name)
  (set-highlighter (setting ::default-highlighter))
  (set-keymap (setting ::default-keymap)))

(defn set-frame-dimensions
  "Setting rows and columns of the window frame."
  [rows columns]
  (swap! editor assoc ::frame-dimensions {::rows rows ::columns columns}
                        ::windows '())
  (add-window "scratch" 1 1 (- rows 1) (- columns 3) "scratch")
  (new-buffer "-prompt-")
  (new-buffer "scratch"))

(defn find-file
  "Opens the file with the given name
  with the default app."
  [filepath]
  ((setting ::default-app) filepath))

(defn create-buffer-from-file
  "Creates a new buffer, connects it to the
  given filepath and loads the content into
  the buffer."
  [filepath]
  (if (not (get-buffer filepath))
    (do
      (swap! editor update ::buffers conj (buffer/create-from-file filepath))
      (switch-to-buffer filepath)
      (set-keymap (setting ::default-keymap))
      (set-highlighter (setting ::default-highlighter)))
    (switch-to-buffer filepath)))

(defn save-file
  "If the current buffer is connected to a file,
  saves the content of the buffer to the
  file."
  []
  (doto-buffer buffer/save-buffer))

(defn copy-selection
  "If there is a selection, the selected
  text will be send to clipboard."
  []
  (when-let [r (get-selection)]
    (util/set-clipboard-content r)
    true))

(defn copy-file
  "If the current buffer is connected to a file,
  the filename is send to the clipboard."
  []
  (when-let [f (get-filename)]
    (util/set-clipboard-content f)
    true))

(defn copy-line
  "Sends the current line to the clipboard."
  []
  (when-let [r (get-line)]
    (util/set-clipboard-content r)))

(defn copy-context
  "Send the context to the clipboard.
  The context could be a filename, a
  function, an url depending on how the
  context is resolved."
  []
  (when-let [r ((get-context) :value)]
    (util/set-clipboard-content r)))

(defn delete-line
  "Deletes the current line."
  []
  (copy-line)
  (doto-buffer buffer/delete-line)
  (update-mem-col))

(defn delete-selection
  "If there is a selection, the selected
  content will be deleted."
  []
  (when (copy-selection)
    (doto-buffer buffer/delete-selection)
    (update-mem-col)
    true))

(defn paste
  "Insert the text from the clipboard
  at the current position."
  []
  (insert (util/clipboard-content)))

(defn swap-line-down
  "Swaps the current line with the one below.
  The cursor follows the the current line down."
  []
  (delete-line)
  (insert-line)
  (paste)
  (beginning-of-line))

(defn swap-line-up
  "Swaps the current line with the line above.
  The cursor follows the current line up."
  []
  (delete-line)
  (backward-line)
  (backward-line)
  (insert-line)
  (paste)
  (beginning-of-line))

(defn prompt-input
  [& string]
  (switch-to-buffer "-prompt-")
  (end-of-buffer)
  (insert (str "\n" (str/join " " string)))
  (backward-char 2))

(defn prompt-append
  [& string]
  (when string
    (switch-to-buffer "-prompt-")
    (insert (str (str/join " " string) "\n"))
    (previous-buffer)
    (updated)))

(defn prompt-set
  [& string]
  (switch-to-buffer "-prompt-")
  (clear)
  (insert (str/join " " string))
  (previous-buffer)
  (updated))

(defn evaluate-file-raw
  "Evaluate a given file raw, without using
  with-out-str or other injected functionality.
  If no filepath is supplied the path connected
  to the current buffer will be used."
  ([filepath]
    (try (load-file filepath)
      (catch Exception e (prompt-set (util/pretty-exception e)))))
  ([] (when-let [filepath (get-filename)] (evaluate-file-raw filepath))))

(defn evaluate-file
  ([filepath]
    (let [extension (or (re-find #"(?<=\.)\w*$" filepath) :empty)
          fun (or ((@editor ::file-eval) extension) ((@editor ::file-eval) :default))]
       (when fun
         (prompt-set "")
         (with-redefs [println prompt-append]
           (try
             (println (fun filepath))
                (catch Exception e (println (util/pretty-exception e))))))))
  ([] (when-let [filepath (get-filename)] (evaluate-file filepath))))

(defn eval-sexp
  [sexp]
  (let [isprompt (= (get-name) "-prompt-")
        namespace (or (clojureutil/get-namespace (current-buffer)) "user")]
    (when isprompt (other-window))
    (prompt-set "")
    (with-redefs [println prompt-append]
      (try
        (println
          (load-string
            (str
              "(do (ns " namespace ") (in-ns '"
              namespace
              ") " sexp ")")))
        (catch Exception e (do (logging/log e) (println (util/pretty-exception e)))))
      )))

(defn eval-safe
  [fun]
  (let [output (try
                 (with-out-str (fun))
                 (catch Exception e (do (logging/log e) (util/pretty-exception e))))]
      (when output (prompt-set output))))

(defn eval-last-sexp
  []
  (eval-sexp (or (get-selection) (sexp-at-point))))

(defn search
  []
  (prompt-input (str "(editor/find-next \"""\")")))

(defn find-next
  ([search]
    (set-setting ::searchstring search)
    (doto-buffer buffer/find-next search)
    (update-mem-col))
  ([]
    (doto-buffer buffer/find-next (setting ::searchstring))
    (update-mem-col)))

(defn top-next-headline
  []
  (find-next "\n# ")
  (forward-char 1)
  (top-align-page))

(defn goto-definition
  [funname]
  (let [[alias funstr] (str/split (if (re-find #"/" funname) funname (str "/" funname)) #"/")
        cp (if (> (count alias) 0)
             (clojureutil/get-class-path (current-buffer) alias)
             (re-find #"[^/\n ]*(?=/)" (with-out-str (clojure.repl/find-doc funstr))))
        filepath (when (> (count cp) 0) (clojureutil/get-file-path cp))]
    (when filepath (find-file filepath))
    (beginning-of-buffer)
    (find-next (str "(defn " "" funstr)))) ;)
  
(defn get-available-functions
  []
  (let [content (get-content)]
    (concat 
      (apply concat
             (for [[fullmatch nsmatch aliasmatch] (re-seq #"\[([^ ]*) :as ([^]]*)\]" content)]
               (when (resolve (symbol aliasmatch))
                 (for [f (keys (ns-publics (symbol nsmatch)))]
                   (str "(" aliasmatch "/" f ")")))))
      (map #(str "(" % ")") (keys (ns-publics (symbol "clojure.core")))))))

(defn context-action
  []
  (let [context (get-context)
        type (context :type)
        value (context :value)]
    (prompt-set (str context))
    (prompt-append (str "TYPE:  " "" type))
    (prompt-append (str "VALUE: " "" value))
    (cond (= type :file) (if (.isAbsolute (io/file value))
                           (find-file value)
                           (find-file (str (.getCanonicalPath (io/file (get-folder) value)))))
          (= type :url) (when-let [start-browser (setting :start-browser)]
                          (start-browser value))
          (= type :function) (goto-definition value))))

(defn remove-buffer
  [buffername]
  (swap! editor update ::buffers remove-item ::buffer/name buffername))

(defn kill-buffer
  []
  (let [buffername (get-name)]
    (when (not (buffer/get-dirty (get-buffer buffername)))
      (previous-real-buffer)
      (remove-buffer buffername)
   )
  ))

(defn tmp-test
  []
  (prompt-append "Testing")
  (prompt-append (str "NS: " (or (clojureutil/get-namespace (current-buffer)) "user")))
  ;(doto-buffer buffer/tmp-buffer ((current-window) ::window/columns))
  ;(prompt-append (clojureutil/get-file-path "dk.salza.liq.editor"))
  ;(prompt-append (clojureutil/get-class-path (current-buffer) "window"))
  ;(prompt-append (str/join "\n" (get-available-functions)))
  (prompt-append (str "--" (get-context) "--"))
  ;(prompt-append (str "--" (get-line) "--"))
  ;(prompt-append (str "--" (get-selection) "--"))
  ;(find-file "/tmp/something.txt")
  )


(defn escape
  []
  (selection-cancel)
  (reset! submap nil)
  (request-fullupdate))

(defn force-quit
  []
  (future
    (Thread/sleep 200)
    (print "\033[0;37m\033[2J")
    (print "\033[?25h")
    (flush)
    (util/cmd "/bin/sh" "-c" "stty -echo cooked </dev/tty")
    (util/cmd "/bin/sh" "-c" "stty -echo sane </dev/tty")
    (flush)
    (Thread/sleep 100)
    (println "")
    (Thread/sleep 100)
    (System/exit 0)))

(defn quit
  []
  (let [dirty (dirty-buffers)]
        (if (empty? dirty)
          (force-quit)
          (prompt-set (str "There are dirty buffers:\n\n"
                           (str/join "\n" dirty) "\n\n"
                           "Press C-M-q to quit anyway.")))))

(defn handle-input
  [keyw]
  (swap! keylist conj keyw)
  (when (and @macro-record (not= keyw "H"))
    (swap! macro-seq conj keyw))
  (let [action (if @submap (@submap keyw) (get-action keyw))
        selfins (when (not action) (if @submap (@submap :selfinsert) (get-action :selfinsert)))]
    (cond (map? action) (do
                          (when (and (action :info) (setting ::key-info))
                                (prompt-set (action :info)))
                          (reset! submap action))
          action (do (reset! submap nil)
                     (action))
          (and selfins (= (count keyw) 1))
                 (do (reset! submap nil)
                    (selfins keyw))
          :else (reset! submap nil))
    (updated)))

(defn record-macro
  []
  (if (not @macro-record)
    (do
      (prompt-append "Recording macro")
      (reset! macro-seq '()))
    (prompt-append "Recording finished"))
  (swap! macro-record not))

(defn run-macro
  []
  (when (not @macro-record)
    (doall (map handle-input (reverse @macro-seq)))))

(defn tmp-do-macro
  []
  (doall (map handle-input '(:i :i :j))))

(defn swap-windows
  []
  (let [buffer1 (get-name)]
    (switch-to-buffer "scratch")
    (other-window)
    (let [buffer2 (get-name)]
      (switch-to-buffer buffer1)
      (other-window)
      (switch-to-buffer buffer2))))

(defn prompt-to-tmp
  []
  (switch-to-buffer "-prompt-")
  (let [content (get-content)]
    (other-window)
    (new-buffer "-tmp-")
    (clear)
    (insert content)))

(defn search-files
  [search]
  (new-buffer "-search-files-")
  (kill-buffer)
  (when (> (count (get-folder)) 1) ;; Avoid searching from empty or root folder
    (let [folder (get-folder)
          result (->> folder
                      cshell/lsr
                      (cshell/flrex (re-pattern search))
                      (map #(str/join " : " %))
                      (str/join "\n"))]
      (new-buffer "-search-files-")
      (insert result)
  ;(->> (get-folder) lsr (flrex (re-pattern search)) (map #(str/join " : " %)) p)
  )))

(defn typeahead
  [items tostringfun callback]
  (when-let [typeaheadfn (get-default-typeahead-function)]
    (typeaheadfn items tostringfun callback)))
