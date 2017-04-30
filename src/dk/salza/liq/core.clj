(ns dk.salza.liq.core
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [dk.salza.liq.adapters.tty :as tty]
            [dk.salza.liq.adapters.winttyadapter :as winttyadapter]
            [dk.salza.liq.adapters.jframeadapter :as jframeadapter]
            [dk.salza.liq.adapters.ghostadapter :as ghostadapter]
            [dk.salza.liq.adapters.webadapter :as webadapter]
            [dk.salza.liq.adapters.htmladapter :as htmladapter]
            [dk.salza.liq.syntaxhl.clojuremdhl :as clojuremdhl]
            [dk.salza.liq.tools.fileutil :as fileutil]
            [dk.salza.liq.tools.cshell :as cshell]
            [dk.salza.liq.apps.findfileapp :as findfileapp]
            [dk.salza.liq.apps.textapp :as textapp]
            [dk.salza.liq.apps.promptapp :as promptapp]
            [dk.salza.liq.apps.commandapp :as commandapp]
            [dk.salza.liq.apps.helpapp :as helpapp]
            [dk.salza.liq.extensions.vimapp :as vimapp]
            [dk.salza.liq.extensions.emacsapp :as emacsapp]
            [dk.salza.liq.editor :as editor]
            [dk.salza.liq.window :as window])
            ;[dk.salza.liq.modes.textmode :as textmode]
  (:gen-class))

(defn is-windows
  []
  (re-matches #"(?i)win.*" (System/getProperty "os.name")))

(defn load-user-file
  [path]
  (let [file (and path (fileutil/file path))] ; (fileutil/file (System/getProperty "user.home") ".liq")]
    (if (and file (fileutil/exists? file) (not (fileutil/folder? file)))
      (editor/evaluate-file-raw (str file))
      ;; Just some samples in case there is user file specified
      (let [tmpdir (if (is-windows) "C:\\Temp\\" "/tmp/")]
        (editor/add-to-setting ::editor/searchpaths tmpdir)
        (editor/add-to-setting ::editor/snippets (str "(->> \"" tmpdir "\" ls (lrex #\"something\") p)"))
        (editor/add-to-setting ::editor/files (str tmpdir "tmp.clj"))))))

(defn set-defaults
  []

  ;; Default highlighter
  (editor/set-default-highlighter clojuremdhl/next-face)

  ;; Default keymap
  (editor/set-default-keymap textapp/keymap-navigation)

;  ;; Default mode
;  (editor/set-default-mode (textmode/create clojuremdhl/next-face))

  ;; Default app
  (editor/set-default-app textapp/run)

  ;; Default global keybindings
  (editor/set-global-key :C-space commandapp/run)
  (editor/set-global-key :C-q editor/quit)
  (editor/set-global-key :C-M-q editor/force-quit)
  (editor/set-global-key :C-f #(findfileapp/run textapp/run))
  (editor/set-global-key :C-o editor/other-window)
  (editor/set-global-key :C-r #(editor/prompt-append "test"))
  (editor/set-global-key :C-h {:a helpapp/help-apropos
                               :f helpapp/help-function
                               :k helpapp/help-key})

  ;; Default interactive functions
  (editor/add-interactive ":w" editor/save-file)
  (editor/add-interactive ":q!" editor/force-quit)
  (editor/add-interactive ":q" editor/quit)
  (editor/add-interactive ":o" editor/find-file)
  ;(editor/add-interactive "eval-last-sexp" editor/eval-last-sexp)

  ;; Default evaluation handling
  (editor/set-eval-function "lisp" #(cshell/cmd "clisp" %))
  (editor/set-eval-function "js" #(cshell/cmd "node" %))
  (editor/set-eval-function "py" #(cshell/cmd "python" %))
  (editor/set-eval-function "r" #(cshell/cmd "R" "-q" "-f" %))
  (editor/set-eval-function "c" #(cshell/cmd "tcc" "-run" %))
  (editor/set-eval-function "tex" #(cshell/cmd "pdflatex" "-halt-on-error" "-output-directory=/tmp" %))
  (editor/set-eval-function :default #(str (load-file %))))

(defn init-editor
  [rows columns]
  ;; Setup start windows and scratch buffer
  (editor/add-window (window/create "prompt" 1 1 rows 40 "-prompt-"))
  (editor/new-buffer "-prompt-")
  (editor/add-window (window/create "main" 1 44 rows (- columns 46) "scratch")) ; todo: Change to percent given by setting. Not hard numbers
  (editor/new-buffer "scratch")
  (editor/insert (str "# Welcome to λiquid\n"
                      "To quit press C-q. To escape situation press C-g. To undo press u in navigation mode (blue cursor)\n"
                      "Use tab to switch between insert mode (green cursor) and navigation mode (blue cursor).\n\n"
                      "## Basic navigation\nIn navigation mode (blue cursor):\n\n"
                      "  j: Left\n  l: Right\n  i: Up\n  k: Down\n\n"
                      "  C-space: Command typeahead (escape with C-g)\n"
                      "  C-f: Find file\n\n"
                      "## Evaluation\n"
                      "Place cursor between the parenthesis below and type \"e\" in navigation mode (f5 when using --vim and C-x C-e using --emacs), to evaluate the expression:\n"
                      "(range 10 30)\n"
                      "(editor/end-of-buffer)\n"
                     ))
  (editor/end-of-buffer))


(defn read-arg
  "Reads the value of an argument.
  If the argument is on the form --arg=value
  then (read-args args \"--arg=\") vil return
  value.
  If the argument is on the form --arg then
  non-nil will bereturned if the argument exists
  otherwise nil."
  [args arg]
  (first (filter identity
                 (map #(re-find (re-pattern (str "(?<=" arg ").*"))
                                %)
                      args))))

(defn read-arg-int
  [args arg]
  (let [strres (read-arg args arg)]
    (when strres (Integer/parseInt strres))))

(defn print-help-and-exit
  []
  (println (str/join "\n" '(
     ""
     "Salza Liquid Help"
     "================="
     ""
     "Examples:"
     "No arguments: Runs in terminal on Linux and Mac and JFrame on Windows."
     "--tty: Force run in terminal. May be used with parameters that normally would disable tty."
     "--server: Starts a server on http://localhost:8520"
     "--server --port=8000: Start a server on http://localhost:8000"
     "--jframe: Runs in JFrame. On linux it needs Inconsolata font and Console font on Windows."
     "--rows=50 --columns=80: Sets number of rows and columns."
     "--no-init-file: Do not read .liq file."
     "--autoupdate: If there are multiple views they should be syncronised."
     "--load=<path to file>: Load <path to file> as init file."
     ""
     "Some parameters may be combined, like:"
     "--tty --jframe --server --port=7000 --no-init-file --rows=50 --columns=80"
  )))
  (System/exit 0))
  

(defn -main
  [& args]
  (if (read-arg args "--help")
    (print-help-and-exit)
    (let [usetty (or (read-arg args "--tty") (not (or (read-arg args "--server") (read-arg args "--ghost") (read-arg args "--jframe"))))
          rows (or (read-arg-int args "--rows=") (and usetty (not (is-windows)) (tty/rows))  40)
          columns (or (read-arg-int args "--columns=") (and usetty (not (is-windows)) (tty/columns)) 140)
          port (or (read-arg-int args "--port=") 8520)
          autoupdate (if (read-arg args "--autoupdate") true false)
          userfile (when-not (read-arg args "--no-init-file") 
                     (or (read-arg args "--load=")
                     (fileutil/file (System/getProperty "user.home") ".liq")))
          singlethreaded (read-arg args "--no-threads")]
          (set-defaults)
          (when (read-arg args "--vim") (vimapp/init))
          (when (read-arg args "--emacs") (emacsapp/init))
          (init-editor (- rows 1) columns)
          (load-user-file userfile)
          (when usetty
            (if (is-windows)
              (jframeadapter/init rows columns)
              (do
                (tty/view-init)
                (tty/input-handler))))
          (when (or (read-arg args "--web") (read-arg args "--server"))
            (((webadapter/adapter rows columns autoupdate) :init) port))
          (when (read-arg args "--html")
            (((htmladapter/adapter rows columns autoupdate) :init) port))
          (when (read-arg args "--jframe")
            (jframeadapter/init rows columns))
          (editor/updated)
       )))
    