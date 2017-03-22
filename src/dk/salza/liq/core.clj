(ns dk.salza.liq.core
  (:require [clojure.java.io :as io]
            [dk.salza.liq.adapters.ttyadapter :as ttyadapter]
            [dk.salza.liq.adapters.winttyadapter :as winttyadapter]
            [clojure.string :as str]
            [dk.salza.liq.adapters.jframeadapter :as jframeadapter]
            [dk.salza.liq.adapters.ghostadapter :as ghostadapter]
            [dk.salza.liq.adapters.webadapter :as webadapter]
            [dk.salza.liq.editor :as editor]
            [dk.salza.liq.window :as window]
            [dk.salza.liq.tools.fileutil :as fileutil]
            [dk.salza.liq.tools.cshell :as cshell]
            [dk.salza.liq.apps.findfileapp :as findfileapp]
            [dk.salza.liq.apps.textapp :as textapp]
            [dk.salza.liq.syntaxhl.clojuremdhl :as clojuremdhl]
            [dk.salza.liq.apps.promptapp :as promptapp]
            [dk.salza.liq.apps.commandapp :as commandapp]
            [dk.salza.liq.modes.textmode :as textmode])
  (:gen-class))

(def adapter (ref nil))

(defn load-user-file
  [path]
  (let [file (fileutil/file path)] ; (fileutil/file (System/getProperty "user.home") ".liq")]
    (if (and (fileutil/exists? file) (not (fileutil/folder? file)))
      (editor/evaluate-file-raw (str file))
      ;; Just some samples in case there is user file specified
      (do (editor/add-to-setting ::editor/searchpaths "/tmp")
          (editor/add-to-setting ::editor/snippets "(->> \"/tmp\" ls (lrex #\"something\") p)")
          (editor/add-to-setting ::editor/files "/tmp/tmp.clj")))))
      ;(load-string (slurp (io/resource "liqdefault"))))))
      ;(user/load-default))))

(defn init-editor
  [rows columns userfile]
  (editor/set-default-mode (textmode/create clojuremdhl/next-face))
  (editor/set-default-app textapp/run)
  (editor/set-global-key :C-space commandapp/run)
  ;(editor/set-global-key :C-f #(findfileapp/run editor/find-file))
  (editor/set-global-key :C-f #(findfileapp/run textapp/run))
  (editor/set-global-key :C-o editor/other-window)
  (editor/set-global-key :C-r #(editor/prompt-append "test"))
  (editor/set-eval-function "lisp" #(cshell/cmd "clisp" %))
  (editor/set-eval-function "js" #(cshell/cmd "node" %))
  (editor/set-eval-function "c" #(cshell/cmd "tcc" "-run" %))
  (editor/set-eval-function :default #(str (load-file %)))
  (when userfile (load-user-file userfile))
  (editor/add-window (window/create "prompt" 1 1 rows 40 "-prompt-"))
  (editor/new-buffer "-prompt-")
  ;(editor/set-mode (textmode/create nil))
  (editor/add-window (window/create "main" 1 44 rows (- columns 46) "scratch")) ; todo: Change to percent given by setting. Not hard numbers
  (editor/new-buffer "scratch")
  ;(editor/set-mode (textmode/create nil))
  (editor/insert (str "# Welcome to λiquid\n"
                      "To quit press C-q. To escape situation press C-g. To undo press u in navigation mode (blue cursor)\n"
                      "Use tab to switch between insert mode (green cursor) and navigation mode (blue cursor).\n\n"
                      "## Basic navigation\nIn navigation mode (blue cursor):\n\n"
                      "  j: Left\n  l: Right\n  i: Up\n  k: Down\n\n"
                      "  C-space: Command typeahead (escape with C-g)\n"
                      "  C-f: Find file\n\n"
                      "## Evaluation\n"
                      "Place cursor between the parenthesis below and type \"e\" in navigation mode, to evaluate the expression:\n"
                      "(range 10 30)\n"
                      "(editor/end-of-buffer)\n"
                     ))
  (editor/end-of-buffer))

(defn update-gui
  []
  (let [windows (reverse (editor/get-windows))
        buffers (map #(editor/get-buffer (window/get-buffername %)) windows)
        lineslist (doall (map #(window/render %1 %2) windows buffers))]
        ;(spit "/tmp/lines.txt" (pr-str lineslist)) 
        (when (editor/check-full-gui-update)
          ((@adapter :reset)))
        (doseq [lines lineslist]
          ((@adapter :print-lines) lines))))

(def updater (ref (future nil)))
(def changes (ref 0))

(defn request-update-gui
  []
  (when (future-done? @updater)
    (dosync (ref-set updater
            (future
              (loop [ch @changes]
                (update-gui)
                (when (not= ch @changes) (recur @changes))))))))

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

(defn is-windows
  []
  (re-matches #"(?i)win.*" (System/getProperty "os.name")))

(defn -main
  [& args]
  (dosync (ref-set adapter 
    (cond (read-arg args "--jframe") jframeadapter/adapter
          ;(read-arg args "--web") webadapter/adapter
          (read-arg args "--ghost") (ghostadapter/adapter
                                      (Integer/parseInt (read-arg args "--rows="))
                                      (Integer/parseInt (read-arg args "--columns=")))
          (is-windows) winttyadapter/adapter
           :else ttyadapter/adapter)))
  
  (let [singlethreaded (read-arg args "--no-threads")
        userfile (when-not (read-arg args "--no-init-file") 
                   (or (read-arg args "--load=")
                       (fileutil/file (System/getProperty "user.home") ".liq")))]
    ((@adapter :init))
    (when (read-arg args "--web") (webadapter/init))
    (init-editor (- ((@adapter :rows)) 1) ((@adapter :columns)) userfile)
    (loop []
      (if singlethreaded
        (update-gui)          ; Non threaded version
        (request-update-gui)) ; Threaded version
      (let [input ((@adapter :wait-for-input))]
        (when (= input :C-M-q) ((@adapter :quit)))
        (when (= input :C-q)
          (let [dirty (editor/dirty-buffers)]
            (if (empty? dirty)
              ((@adapter :quit))
              (editor/prompt-set (str "There are dirty buffers:\n\n"
                                      (str/join "\n" dirty) "\n\n"
                                      "Press C-M-q to quit anyway.")))))
        (when (= input :C-space) ((@adapter :reset)))
        (editor/handle-input input)
        (dosync (alter changes inc)))
      (recur))))
    