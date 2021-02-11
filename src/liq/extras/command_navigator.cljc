(ns liq.extras.command-navigator
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.repl :as repl]
            [liq.editor :as editor]
            [liq.buffer :as buffer]
            [liq.util :as util]
            [liq.commands :as commands]))

(defn files-below
  [path]
  (filter #(not (re-find #"\.(png|class|jpg|pdf|git/)" %))
          (filter util/file?
                  (map #(.getAbsolutePath %)
                       (file-seq (io/file (util/resolve-home path)))))))


(defn add-file
  [f]
  (swap! editor/state update ::entries conj {:type :file :command f :caption f}))

(defn add-files-below
  [fo]
  (doseq [f (files-below fo)]
    (add-file f)))

(defn add-folder
  [fo]
  (swap! editor/state update ::entries conj {:type :folder :command (util/resolve-home fo) :caption (util/resolve-home fo)}))

(defn commands-to-entries
  []
  (for [[k v] (@editor/state ::editor/commands)]
    {:type (if (and (-> v meta) (-> v meta :buffer)) :buffer :command)
     :command v
     :caption (str/replace (str k " - " (-> v meta :doc)) #"\n" " ")}))

(defn execute
  [item]
  (cond (= (item :type) :buffer) (editor/apply-to-buffer (item :command))
        (= (item :type) :file) (editor/open-file (item :command))
        (= (item :type) :folder) (((editor/get-mode :dired-mode) :init) (item :command))
        true ((item :command))))

(defn run
  []
  (((editor/get-mode :typeahead-mode) :init)
   (concat (sort-by :caption (commands-to-entries))
           (sort-by :caption (filter #(= (% :type) :folder) (@editor/state ::entries)))
           (sort-by :caption (filter #(= (% :type) :file) (@editor/state ::entries))))
   :caption
   execute))

; (swap! editor/state assoc :liq.extras.command-navigator/entries {})
; (add-file "/tmp/tmp.clj")
; (add-files-below "~/m/pim")
; (files-below "~/m/pim")
; (add-folder "/tmp")
; (-> @editor/state :liq.extras.command-navigator/entries)
; (run)


