(ns dk.salza.liq.core
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [dk.salza.liq.adapters.tty :as tty]
            [dk.salza.liq.adapters.jframeadapter :as jframeadapter]
            [dk.salza.liq.adapters.ghostadapter :as ghostadapter]
            [dk.salza.liq.adapters.webadapter :as webadapter]
            [dk.salza.liq.keymappings.navigation]
            [dk.salza.liq.keymappings.normal]
            [dk.salza.liq.keymappings.insert]
            [dk.salza.liq.syntaxhl.clojuremdhl :as clojuremdhl]
            [dk.salza.liq.tools.fileutil :as fileutil]
            [dk.salza.liq.tools.cshell :as cshell]
            [dk.salza.liq.apps.findfileapp :as findfileapp]
            [dk.salza.liq.apps.textapp :as textapp]
            [dk.salza.liq.apps.promptapp :as promptapp]
            [dk.salza.liq.apps.commandapp :as commandapp]
            [dk.salza.liq.apps.helpapp :as helpapp]
            [dk.salza.liq.apps.typeaheadapp :as typeaheadapp]
            [dk.salza.liq.editor :as editor]
            [dk.salza.liq.logging :as logging])
  (:gen-class))

(def ^:private logo (str/join "\n" (list
  " "
  "       o0o"
  "     o0000"
  "   o000  00"
  "     000  00"
  "      000  00"
  "       000  00"
  "        000  00"
  "         000  00"
  "          000  00"
  "           000  00"
  "            000  00"
  "             000  00"
  "              00   00"
  "             000    00"
  "            000      00"
  "           000        00"
  "          000  o   o   00"
  "         000ooo ooo o oo00"
  "        000  o   o   o   00"
  "       000   o    o    o  00"
  "      000  o    o    o  o  00"
  "     000  o o               00"
  "    000    o  o     o  o     00"
  "   000  o       o     o   o  00o"
  "  o00000000000000000000000000000o"
  " "
  "    o      o              o    o"
  "   o o                         o"
  "      o    o   oo   o  o  o   oo"
  "     o o   o  o  o  o  o  o  o o"
  "    o   o  o   ooo   oo   o   oo"
  "                 o"
  "                 o"
  " "
)))

(defn- is-windows
  []
  (re-matches #"(?i)win.*" (System/getProperty "os.name")))

(defn- load-user-file
  [path]
  (let [file (and path (fileutil/file path))]
    (if (and file (fileutil/exists? file) (not (fileutil/folder? file)))
      (editor/evaluate-file-raw (str file))

      ;; Just some samples in case there is user file specified
      (let [tmpdir (if (is-windows) "C:\\Temp\\" "/tmp/")]
        (editor/add-to-setting ::editor/searchpaths tmpdir)
        (editor/add-to-setting ::editor/snippets (str "(->> \"" tmpdir "\" ls (lrex #\"something\") p)"))
        (editor/add-to-setting ::editor/files (str tmpdir "tmp.clj"))))))

(defn set-defaults
  []

  ;; Load keymaps
  (editor/add-keymap dk.salza.liq.keymappings.navigation/keymapping)
  (editor/add-keymap dk.salza.liq.keymappings.normal/keymapping)
  (editor/add-keymap dk.salza.liq.keymappings.insert/keymapping)

  ;; Default keymap
  (editor/set-default-keymap "dk.salza.liq.keymappings.normal")

  ;; Default highlighter
  (editor/set-default-highlighter clojuremdhl/next-face)

  ;; Default typeahead function
  (editor/set-default-typeahead-function typeaheadapp/run)

  ;; Default app
  (editor/set-default-app textapp/run)

  ;; Default global keybindings
  (editor/set-global-key "C- " #(do (editor/request-fullupdate) (commandapp/run)))
  (editor/set-global-key "C-q" editor/quit)
  (editor/set-global-key "C-M-q" editor/force-quit)
  (editor/set-global-key "C-f" #(findfileapp/run editor/find-file))
  (editor/set-global-key "C-o" editor/other-window)
  (editor/set-global-key "C-r" #(editor/prompt-append "test"))
  (editor/set-global-key "C-h" {:info "h: Browse\na: Apropos\nf: Function\nk: Key"
                               "C-h" #(helpapp/help-browse "index.md")
                               "h" #(helpapp/help-browse "index.md")
                               "a" helpapp/help-apropos
                               "f" helpapp/help-function
                               "k" helpapp/help-key})
  (editor/set-global-key "C-x" "C-c" editor/quit)
  (editor/set-global-key "C-x" "2" editor/split-window-below)
  (editor/set-global-key "C-x" "3" editor/split-window-right)
  (editor/set-global-key "C-x" "C-f" #(findfileapp/run textapp/run))
  (editor/set-global-key "C-x" "o" editor/other-window)
  (editor/set-global-key "C-x" "0" editor/delete-window)
  (editor/set-global-key "C-x" "+" (partial editor/enlarge-window-right 5))
  (editor/set-global-key "C-x" "/" (partial editor/shrink-window-right 5))
  (editor/set-global-key "C-x" "down" (partial editor/enlarge-window-below 1))
  (editor/set-global-key "C-x" "up" (partial editor/shrink-window-below 1))


  ;; Default interactive functions
  (editor/add-interactive ":w" editor/save-file)
  (editor/add-interactive ":q!" editor/force-quit)
  (editor/add-interactive ":q" editor/quit)
  (editor/add-interactive ":o" #(findfileapp/run editor/find-file))
  (editor/add-interactive ":edit" #(findfileapp/run editor/find-file))
  (editor/add-interactive ":e" #(findfileapp/run editor/find-file))
  (editor/add-interactive ":bd" editor/kill-buffer)

  (editor/add-command "SPC mee (eval-last-sexp)" editor/eval-last-sexp)
  (editor/add-command "SPC meb (evaluate-file)" editor/evaluate-file)

  (editor/set-spacekey ["m"] "Clojure commands" nil)
  (editor/set-spacekey ["m" "e"] "Evaluation" nil)
  (editor/set-spacekey ["m" "e" "e"] "eval-last-sexp" editor/eval-last-sexp)
  (editor/set-spacekey ["m" "e" "b"] "eval-buffer" editor/evaluate-file)
  (editor/set-spacekey ["m" "g"] "Goto" nil)
  (editor/set-spacekey ["m" "g" "g"] "Goto definition" editor/context-action)
  (editor/set-spacekey ["m" ","] "Highligt sexp" editor/highlight-sexp-at-point)
  (editor/set-spacekey ["f"] "Files" nil)
  (editor/set-spacekey ["f" "f"] "find-file" #(findfileapp/run textapp/run))
  (editor/set-spacekey ["f" "s"] "save-file" editor/save-file)
  (editor/set-spacekey ["w"] "Window" nil)
  (editor/set-spacekey ["w" "/"] "Split window vertically" editor/split-window-right)
  (editor/set-spacekey ["w" "-"] "Split window horizontally" editor/split-window-below)
  (editor/set-spacekey ["w" "d"] "Delete window" editor/delete-window)
  (editor/set-spacekey ["w" "w"] "Other window" editor/other-window)
  (editor/set-spacekey ["q"] "Quit" nil)
  (editor/set-spacekey ["q" "q"] "Quit" editor/quit)
  (editor/set-spacekey ["\t"] "Last buffer" editor/previous-real-buffer)
  (editor/set-spacekey [" "] "Command typeahead" #(do (editor/request-fullupdate) (commandapp/run)))


  (editor/add-interactive "apropos" clojure.repl/find-doc "APROPOS")
  (editor/add-interactive "Reopen files changed on disk" editor/reopen-all-files)

  ;; Default searchpaths
  (when (fileutil/exists? "project.clj")
    (editor/add-searchpath (fileutil/canonical ".")))

  ;; Default evaluation handling
  (editor/set-eval-function "lisp" #(cshell/cmd "clisp" %))
  (editor/set-eval-function "js" #(cshell/cmd "node" %))
  (editor/set-eval-function "py" #(cshell/cmd "python" %))
  (editor/set-eval-function "r" #(cshell/cmd "R" "-q" "-f" %))
  (editor/set-eval-function "c" #(cshell/cmd "tcc" "-run" %))
  (editor/set-eval-function "hs" #(cshell/cmd "stack" %))
  (editor/set-eval-function "cljs" #(cshell/cmd "lumo" %))
  (editor/set-eval-function "sh" #(cshell/cmd "bash" %))
  (editor/set-eval-function "tex" #(cshell/cmd "pdflatex" "-halt-on-error" "-output-directory=/tmp" %))
  (editor/set-eval-function :default #(str (load-file %))))

(defn init-editor
  []
  ;; Setup windows
  (editor/split-window-right 0.22)
  (editor/switch-to-buffer "-prompt-")
  (editor/insert logo)
  (editor/other-window)
  (editor/switch-to-buffer "scratch")
  (editor/insert (str "# Welcome to λiquid\n"
                      "To quit press C-q. To escape situation press C-g."
                      "To undo press u in navigation mode (blue cursor)\n"
                      "Keybindings are very similar to VIM and Fireplace\n"
                      "Use Esc to switch to normal mode (blue cursor).\n\n"
                      "## Basic navigation\nIn navigation mode (blue cursor):\n\n"
                      "  h: Left\n  l: Right\n  k: Up\n  j: Down\n\n"
                      "  space: Spacemacs like feature\n"
                      "  C-space: Command typeahead (escape with C-g)\n"
                      "  C-x C-f: Find file (or space f f)\n\n"
                      "## Evaluation\n"
                      "Place cursor between the parenthesis below and type \"c p p\" "
                      "in normal mode, "
                      "to evaluate the expression:\n"
                      "(range 10 30)\n"
                      "(editor/end-of-buffer)\n\n"
                      "## Windows\n"
                      "If you use Windows and have no experience with vim keys, press \"i\" to\n"
                      "make the cursor green. Now use arrow keys to move around\n"
                      "and f5 to evaluate s-expressions.\n"
                     ))
  (editor/end-of-buffer))

(defn- read-arg
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

(defn- read-arg-int
  [args arg]
  (let [strres (read-arg args arg)]
    (when strres (Integer/parseInt strres))))

(defn- print-help-and-exit
  []
  (println (str/join "\n" (list
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
     "--log=<path to file>: Will write log information to the <path to file>."
     "--minimal: To prevent loading some default settings. Useful for fully cusomizing Liquid."
     ""
     "Some parameters may be combined, like:"
     "--tty --jframe --server --port=7000 --no-init-file --rows=50 --columns=80 --log=/tmp/liq.log"
  )))
  (System/exit 0))

(defn- print-version-and-exit
  []
  (let [proj (clojure.java.io/resource "project.clj")]
    (if proj
      (println (re-find #"(?<=liquid \")[^\"]*(?=\")" (slurp proj)))
      (println "Version can only be extracted from jar.")))
  (System/exit 0))
  
(defn startup
  [& args]
  ;; If arguments are --help or --version, just show information
  ;; and quit.
  (cond (read-arg args "--help") (print-help-and-exit)
        (read-arg args "--version") (print-version-and-exit)
    :else
    (let [;easy (read-arg args "--easy")
          useserver (or (read-arg args "--server")
                        (read-arg args "--web"))
          usejframe (or (read-arg args "--jframe")
                        ;(read-arg args "--easy")
                        (and (not (read-arg args "--ghost")) (is-windows)))
          usetty (or (read-arg args "--tty") (not (or usejframe useserver (read-arg args "--ghost"))))
          rows (or (read-arg-int args "--rows=")
                   (and usetty (tty/rows))
                   40)
          columns (or (read-arg-int args "--columns=")
                      (and usetty (tty/columns))
                      140)
          port (or (read-arg-int args "--port=") 8520)
          autoupdate (if (read-arg args "--autoupdate") true false)
          fontsize (read-arg-int args "--fontsize=")
          logfile (read-arg args "--log=")
          singlethreaded (read-arg args "--no-threads")]

          (when logfile
            (logging/enable logfile))

          (set-defaults)
          (editor/set-frame-dimensions rows columns)

          (when usetty
            (editor/set-setting :tty true)
            (tty/view-init)
            (tty/input-handler))

          (when usejframe
            (jframeadapter/init rows columns :font-size fontsize))

          (when useserver
            (((webadapter/adapter rows columns autoupdate) :init) port))

          (editor/updated))))
    
(defn -main
  [& args]
  (apply startup args)
  (let [;easy (read-arg args "--easy")
        minimal (read-arg args "--minimal")
        f1 (fileutil/file (System/getProperty "user.home") ".liquid")
        f2 (fileutil/file (System/getProperty "user.home") ".liq")
        userfile (when-not (read-arg args "--no-init-file") 
                   (or (read-arg args "--load=")
                     (and (not minimal)
                          (if (.exists (io/file f1)) f1 f2))))]
    (cond minimal (do)
          ;easy (init-editor-easy)
          true (init-editor))
    (load-user-file userfile)
    (when-let [filename (re-matches #"[^-].*" (or (last args) ""))]
      (editor/find-file filename))
    (editor/updated)))
