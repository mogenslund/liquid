(ns dk.salza.liq.editor
  (:require [dk.salza.liq.buffer :as buffer]
            [dk.salza.liq.window :as window]
            [dk.salza.liq.tools.util :as util]
            [dk.salza.liq.clojureutil :as clojureutil]
            [clojure.java.io :as io]
            [dk.salza.liq.coreutil :refer :all]
            [dk.salza.liq.logging :as logging]
            [dk.salza.liq.tools.cshell :as cshell]
            [clojure.string :as str]))

(def top-of-window (atom {})) ; Keys are windowname-buffername
(def default-highlighter (atom nil))
(def default-keymap (atom nil))
(def default-app (atom nil))
(def searchstring (atom ""))
(def macro-seq (atom '()))
(def updates (atom 0))
(def submap (atom nil))

(def macro-record (atom false))

(def empty-editor {::buffers '()
                   ::windows '()
                   ::global-keymap {}
                   ::file-eval {}
                   ::settings {::searchpaths '()
                               ::files '()
                               ::snippets '()
                               ::commands '()
                               ::interactive '()}})

(def editor (ref empty-editor))

(defn reset
  "Resets the editor. Mostly for testing purposes."
  []
  (dosync
    (ref-set editor empty-editor)))

(defn updated
  "Call this function to proclaim that an
  update has been made to the editor.
  This can be used by views to check for updates."
  []
  (swap! updates inc)
  nil)


(defn set-default-keymap
  "Set the keymap to be used as default when a
  new buffer is created."
  [keymap]
  (reset! default-keymap keymap))

(defn set-default-highlighter
  "Set the highlighter function to be used as
  default when a new buffer is created."
  [highlighter]
  (reset! default-highlighter highlighter))

(defn get-default-highlighter
  []
  @default-highlighter)

(defn set-default-app
  "Set the default app to be used when a new
  buffer is created."
  [app]
  (reset! default-app app))

(defn doto-buffer
  "Apply the given function to the top-most buffer."
  [fun & args]
  (dosync
    (alter editor update ::buffers #(apply doto-first (list* % fun args))))
  nil)

(defn doto-window
  "Apply the given function to the current."
  [fun & args]
  (dosync
    (alter editor update ::windows #(apply doto-first (list* % fun args))))
  (updated)
  nil)

(defn current-buffer
  "Returns the current active buffer."
  []
  (-> @editor ::buffers first))

(defn current-window
  "Get the current active window."
  []
  (-> @editor ::windows first))

(defn setting
  "Get the setting with the given key."
  [keyw]
  (-> @editor ::settings keyw))

(defn add-to-setting
  [keyw entry]
  (when (not (some #{entry} (setting keyw)))
    (dosync (alter editor update-in [::settings keyw] conj entry))))

(defn set-setting
  "Set keyw to value in the
  settings part of the editor." 
  [keyw value]
  (dosync (alter editor assoc-in [::settings keyw] value)))

(defn set-global-key
  [keyw fun]
  (dosync (alter editor assoc-in [::global-keymap keyw] fun)) nil)

(defn set-eval-function
  "Associate an extension with a function. The function
  is assumed to take one input - the filepath.
  EXAMPLE (To associate files with \"py\" extension with the
           python command):
    (editor/set-eval-function \"py\" #(cshell/cmd \"python\" %))"
  [extension fun]
  (dosync (alter editor assoc-in [::file-eval extension] fun)) nil)

(defn add-command
  "Add a command to be availble for commandapp typeahead.
  add-interactive is in most cases more suitable."
  [fun]
  (add-to-setting ::commands fun) nil)

(defn add-searchpath
  "Add a folder to searchpath.
  When using the commandapp files below
  folders in the searchpath will be available
  through typeahead.
  EXAMPLE: (editor/add-searchpath \"/tmp\")"
  [s]
  (add-to-setting ::searchpaths s) nil)

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
  [label fun & arglabels]
  (add-to-setting ::interactive [label fun arglabels])
  nil)

(defn add-window [window] (dosync (alter editor update ::windows conj window)))
(defn get-windows [] (@editor ::windows))

(defn get-buffer
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
  [buffername]
  (dosync
    (alter editor update ::buffers bump ::buffer/name buffername)
    (let [win (get-match (get-windows) ::window/buffername buffername)]
      (if win
        (alter editor update ::windows bump ::window/buffername buffername)
        (alter editor update ::windows doto-first assoc ::window/buffername buffername)))))

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


(defn get-visible-content [] (-> (current-buffer) buffer/get-visible-content))

(defn insert
  "Insert a string to the current active buffer
  at the cursor position."
  [string]
  (doto-buffer buffer/insert string) (update-mem-col))

(defn insert-line [] (doto-buffer buffer/insert-line) (update-mem-col))
(defn forward-char ([amount] (doto-buffer buffer/forward-char amount) (update-mem-col))
                   ([]       (doto-buffer buffer/forward-char 1) (update-mem-col)))
(defn forward-word [] (doto-buffer buffer/forward-word) (update-mem-col))
(defn end-of-word [] (doto-buffer buffer/end-of-word) (update-mem-col))
(defn backward-char ([amount] (doto-buffer buffer/backward-char amount) (update-mem-col))
                    ([]       (doto-buffer buffer/backward-char 1) (update-mem-col)))
(defn delete ([amount] (doto-buffer buffer/delete amount) (update-mem-col))
             ([] (doto-buffer buffer/delete 1) (update-mem-col)))
(defn delete-char [] (doto-buffer buffer/delete-char) (update-mem-col))
(defn replace-char [s] (delete-char) (insert s) (backward-char))
(defn end-of-line [] (doto-buffer buffer/end-of-line) (update-mem-col))
(defn beginning-of-line [] (doto-buffer buffer/beginning-of-line) (update-mem-col))
(defn beginning-of-buffer [] (doto-buffer buffer/beginning-of-buffer) (update-mem-col))
(defn end-of-buffer [] (doto-buffer buffer/end-of-buffer) (update-mem-col))
(defn clear [] (doto-buffer buffer/clear))
(defn selection-set [] (doto-buffer buffer/set-mark "selection"))
(defn selection-cancel [] (doto-buffer buffer/remove-mark "selection"))
(defn selection-toggle
  []
  (if (buffer/get-mark (current-buffer) "selection")
    (selection-cancel)
    (selection-set)))
(defn select-sexp-at-point [] (doto-buffer buffer/select-sexp-at-point))
(defn highlight-sexp-at-point [] (doto-buffer buffer/highlight-sexp-at-point))
(defn undo [] (doto-buffer buffer/undo))

(defn get-selection [] (-> (current-buffer) buffer/get-selection))
(defn get-content [] (-> (current-buffer) buffer/get-content))
(defn sexp-at-point [] (-> (current-buffer) buffer/sexp-at-point))
(defn get-context [] (-> (current-buffer) buffer/get-context))
(defn get-line [] (-> (current-buffer) buffer/get-line))
(defn get-char [] (-> (current-buffer) buffer/get-char))
(defn get-name [] (-> (current-buffer) buffer/get-name))
(defn get-point [] (-> (current-buffer) buffer/get-point))
(defn set-mark [name] (doto-buffer buffer/set-mark name))
(defn get-mark [name] (-> (current-buffer) (buffer/get-mark name)))
(defn remove-mark [name] (doto-buffer buffer/remove-mark name))
(defn point-to-mark [name] (doto-buffer buffer/point-to-mark name))
(defn end-of-buffer? [] (-> (current-buffer) buffer/end-of-buffer?))

(defn forward-line
  "Move cursor forward one line
  in the current active buffer."
  []
  (doto-buffer buffer/forward-visual-line ((current-window) ::window/columns)))

(defn backward-line
  "Move cursor backward one line
  in the current active buffer."
  []
  (doto-buffer buffer/backward-visual-line ((current-window) ::window/columns)))

(defn forward-page
  []
  (let [towid (window/get-towid (current-window) (current-buffer))]
    (doto-buffer buffer/set-point (or (@top-of-window towid)  0))
    (dotimes [n ((current-window) ::window/rows)] (forward-line))
    (beginning-of-line)
    (swap! top-of-window assoc towid (get-point))))

(defn top-align-page
  []
  (let [towid (window/get-towid (current-window) (current-buffer))]
    (beginning-of-line)
    (swap! top-of-window assoc towid (get-point))))


(defn other-window
  []
  (let [buffername (-> @editor ::windows second (window/get-buffername))]
    (dosync (alter editor update ::windows rotate)
            (alter editor update ::buffers bump ::buffer/name buffername))))

(defn reduce-window-width [] (doto-window window/resize-width -1))
(defn enlarge-window-width [] (doto-window window/resize-width 1))

(defn reduce-window-height [] (doto-window window/resize-height -1))
(defn enlarge-window-height [] (doto-window window/resize-height 1))

(defn window-split-right
  []
  (let [curwin (current-window)
        half (quot (curwin ::window/columns) 2)]
    (doto-window window/resize-width (- -2 half))
    (add-window (window/create
                  (str (curwin ::window/name) "-right")
                  (curwin ::window/top)
                  (+ (curwin ::window/left) half 2)
                  (curwin ::window/rows)
                  (- (curwin ::window/columns) half 3)
                  (curwin ::window/buffername))))
  nil)

(defn previous-buffer
  []
  (switch-to-buffer (-> @editor ::buffers second ::buffer/name)))

(defn previous-real-buffer
  []
  (when-let [previous (first (filter (fn [x] (not (re-find #"^-" x))) (rest (buffer-names))))]
    (switch-to-buffer previous)))

(defn set-keymap
  [keymap]
  (doto-buffer buffer/set-keymap keymap))

(defn get-keymap
  []
  (buffer/get-keymap (current-buffer)))

(defn set-highlighter
  [highlighter]
  (doto-buffer buffer/set-highlighter highlighter))

(defn get-highlighter
  []
  (buffer/get-highlighter (current-buffer)))

(defn get-action
  [keyw]
  (or (buffer/get-action (current-buffer) keyw)
      (-> @editor ::global-keymap keyw)))

(defn new-buffer
  [name]
  (when (not (get-buffer name))
    (dosync
      (alter editor update ::buffers conj (buffer/create name))))
  (switch-to-buffer name)
  (set-highlighter @default-highlighter)
  (set-keymap @default-keymap))

(defn find-file
  [filepath]
  (@default-app filepath))

(defn create-buffer-from-file
  [filepath]
  (if (not (get-buffer filepath))
    (do
      (dosync
        (alter editor update ::buffers conj (buffer/create-from-file filepath)))
      (switch-to-buffer filepath)
      (set-keymap @default-keymap)
      (set-highlighter @default-highlighter))
    (switch-to-buffer filepath)))

(defn save-file [] (doto-buffer buffer/save-buffer))

(defn copy-selection
  []
  (when-let [r (get-selection)]
    (util/set-clipboard-content r)
    true))

(defn copy-file
  []
  (when-let [f (get-filename)]
    (util/set-clipboard-content f)
    true))

(defn copy-line
  []
  (when-let [r (get-line)]
    (util/set-clipboard-content r)))

(defn copy-context
  []
  (when-let [r ((get-context) :value)]
    (util/set-clipboard-content r)))

(defn delete-line
  []
  (copy-line)
  (doto-buffer buffer/delete-line)
  (update-mem-col))

(defn delete-selection
  []
  (when (copy-selection)
    (doto-buffer buffer/delete-selection)
    (update-mem-col)
    true))

(defn paste
  []
  (insert (util/clipboard-content)))

(defn swap-line-down
  []
  (delete-line)
  (insert-line)
  (paste)
  (beginning-of-line))

(defn swap-line-up
  []
  (delete-line)
  (backward-line)
  (backward-line)
  (insert-line)
  (paste)
  (beginning-of-line))

(defn prompt-append
  [& string]
  (when string
    (switch-to-buffer "-prompt-")
    (insert (str (str/join " " string) "\n"))
    (previous-buffer)
    (updated)))

(defn prompt-input
  [& string]
  (switch-to-buffer "-prompt-")
  (end-of-buffer)
  (insert (str "\n" (str/join " " string)))
  (backward-char 2))

(defn prompt-set
  [& string]
  (switch-to-buffer "-prompt-")
  (clear)
  (insert (str/join " " string))
  (previous-buffer)
  (updated))

(defn evaluate-file-raw
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
    (reset! searchstring search)
    (doto-buffer buffer/find-next search)
    (update-mem-col))
  ([]
    (doto-buffer buffer/find-next @searchstring)
    (update-mem-col)))

(defn top-next-headline
  []
  (find-next "\n# ")
  (forward-char 1)
  (top-align-page))
  
(defn goto-definition
  [funname]
  (if (re-find #"/" funname)
    (let [[alias funstr] (str/split funname #"/")
          filepath (clojureutil/get-file-path (clojureutil/get-class-path (current-buffer) alias))]
      (find-file filepath)
      (goto-definition funstr))
  (do
    (beginning-of-buffer)
    (find-next (str "(defn " "" funname)))))

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
  (dosync
    (alter editor update ::buffers remove-item ::buffer/name buffername)))

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
  (reset! submap nil))

(defn force-quit
  []
  (print "\033[0;37m\033[2J")
  (print "\033[?25h")
  (flush)
  (util/cmd "/bin/sh" "-c" "stty -echo cooked </dev/tty")
  (util/cmd "/bin/sh" "-c" "stty -echo sane </dev/tty")
  (flush)
  (Thread/sleep 100)
  (println "")
  (Thread/sleep 100)
  (System/exit 0))

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
  (when (and @macro-record (not= keyw :H))
    (swap! macro-seq conj keyw))
  (let [action (if @submap (@submap keyw) (get-action keyw))]
    (cond (map? action) (reset! submap action)
          action (do (reset! submap nil)
                     (action))
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