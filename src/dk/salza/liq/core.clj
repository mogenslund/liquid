(ns dk.salza.liq.core
  (:require [clojure.java.io :as io]
            [dk.salza.liq.apis :refer :all]
            [dk.salza.liq.adapters.ttyadapter]
            ;[user :as user]
            [clojure.string :as str]
            [dk.salza.liq.adapters.jframeadapter]
            [dk.salza.liq.editor :as editor]
            [dk.salza.liq.window :as window]
            [dk.salza.liq.fileutil :as fileutil]
            [dk.salza.liq.modes.promptmode :as promptmode]
            [dk.salza.liq.modes.plainmode :as plainmode])
  (:import (dk.salza.liq.adapters.ttyadapter TtyAdapter)
           (dk.salza.liq.adapters.jframeadapter JframeAdapter))
  (:gen-class))

(defn load-user-file
  [path]
  (let [file (fileutil/file path)] ; (fileutil/file (System/getProperty "user.home") ".liq")]
    (if (and (fileutil/exists? file) (not (fileutil/folder? file)))
      (editor/evaluate-file-raw (str file))
      (do (editor/add-to-setting ::editor/searchpaths "/tmp")
          (editor/add-to-setting ::editor/snippets "(->> \"/tmp\" ls (lrex #\"something\") p)")
          (editor/add-to-setting ::editor/files "/tmp/tmp.clj")))))
      ;(load-string (slurp (io/resource "liqdefault"))))))
      ;(user/load-default))))

(defn init-editor
  [rows columns userfile]
  (editor/init)
  (editor/set-default-mode plainmode/mode)
  (when userfile (load-user-file userfile))
  (editor/add-window (window/create "prompt" 1 1 rows 40 "-prompt-"))
  (editor/new-buffer "-prompt-")
  ;(editor/set-mode plainmode/mode)
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
                      "Place cursor between the parenthesis below and type \"e\" in navigation mode, to evaluate the expression:\n"
                      "(range 10 30)\n"
                      "(editor/end-of-buffer)\n"
                     ))
  ;(editor/set-mode plainmode/mode)
  (editor/end-of-buffer))

(defn update-gui
  [adapter]
  (let [windows (reverse (editor/get-windows))
        buffers (map #(editor/get-buffer (window/get-buffername %)) windows)
        lineslist (doall (map #(window/render %1 %2) windows buffers))]
        ;(spit "/tmp/lines.txt" (pr-str lineslist)) 
        (doseq [lines lineslist]
          (print-lines adapter lines))))

(def updater (atom (future nil)))
(def changes (atom 0))

(defn request-update-gui
  [adapter]
  (when (future-done? @updater)
    (reset! updater
            (future
              (loop [ch @changes]
                (update-gui adapter)
                (when (not= ch @changes) (recur @changes)))))))

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

(defn -main
  [& args]
  (let [adapter (if (read-arg args "--jframe") (JframeAdapter.) (TtyAdapter.))
        singlethreaded (read-arg args "--no-threads")
        userfile (when-not (read-arg args "--no-init-file") 
                   (or (read-arg args "--load=")
                       (fileutil/file (System/getProperty "user.home") ".liq")))]
    (init adapter)
    (init-editor (- (rows adapter) 1) (columns adapter) userfile)
    (loop []
      (if singlethreaded
        (update-gui adapter)          ; Non threaded version
        (request-update-gui adapter)) ; Threaded version
      (let [input (wait-for-input adapter)]
        (when (= input :C-M-q) (quit adapter))
        (when (= input :C-q)
          (let [dirty (editor/dirty-buffers)]
            (if (empty? dirty)
              (quit adapter)
              (editor/prompt-set (str "There are dirty buffers:\n\n"
                                      (str/join "\n" dirty) "\n\n"
                                      "Press C-M-q to quit anyway.")))))
        (when (= input :C-space) (reset adapter))
        (editor/handle-input input)
        (swap! changes inc))
      (recur))))
    