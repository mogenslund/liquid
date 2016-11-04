(ns dk.salza.liq.core
  (:require [clojure.java.io :as io]
            [dk.salza.liq.apis :refer :all]
            [dk.salza.liq.adapters.ttyadapter]
            ;[user :as user]
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
  []
  (let [file (fileutil/file (System/getProperty "user.home") ".liq")]
    (if (and (fileutil/exists? file) (not (fileutil/folder? file)))
      (editor/evaluate-file (str file))
      (do (editor/add-to-setting ::editor/searchpaths "/tmp")
          (editor/add-to-setting ::editor/snippets "(->> \"/tmp\" ls (lrex #\"something\") p)")
          (editor/add-to-setting ::editor/files "/tmp/tmp.clj")))))
      ;(load-string (slurp (io/resource "liqdefault"))))))
      ;(user/load-default))))

(defn init-editor
  [rows columns]
  (editor/init)
  (editor/set-default-mode plainmode/mode)
  (editor/add-window (window/create "prompt" 1 1 rows 40 "-prompt-"))
  (editor/new-buffer "-prompt-")
  ;(editor/set-mode plainmode/mode)
  (editor/add-window (window/create "main" 1 44 rows (- columns 46) "scratch")) ; todo: Change to percent given by setting. Not hard numbers
  (editor/new-buffer "scratch")
  (editor/insert "Welcome to λiquid\n-----------------\n")
  ;(editor/set-mode plainmode/mode)
  (editor/end-of-buffer)
  (load-user-file))

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

(defn -main
  [& args]
  (let [adapter (if (and (> (count args) 0) (= (first args) "jframe"))  (JframeAdapter.) (TtyAdapter.))]
    (init adapter)
    (init-editor (- (rows adapter) 1) (columns adapter))
    (loop []
      (request-update-gui adapter) ; Threaded version
      ;(update-gui adapter) ; Non threaded version
      (let [input (wait-for-input adapter)]
        (when (= input :C-q) (quit adapter))
        (when (= input :C-space) (reset adapter))
        (editor/handle-input input)
        (swap! changes inc))
      (recur))))
    