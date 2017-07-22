(ns dk.salza.liq.apps.findfileapp
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [dk.salza.liq.editor :as editor]
            [dk.salza.liq.keys :as keys]
            [dk.salza.liq.tools.fileutil :as fileutil]
            [dk.salza.liq.coreutil :refer :all]))

(def state (atom {}))

(defn reset-state
  [path]
  (swap! state assoc
               ::search ""
               ::path (or path (.getAbsolutePath (io/file "")))
               ::selected nil
               ::hit nil))

(defn update-display
  []
  (let [pat (re-pattern (str "(?i)" (str/replace (@state ::search) #" " ".*") "[^/]*$"))
        filterfun (filter #(re-find pat (str %))) ; transducer
        folders (sort-by count (filter #(re-find pat %) (conj (sort-by str/upper-case (fileutil/get-folders (@state ::path))) "..")))
        files (sort-by count (filter #(re-find pat %) (sort-by str/upper-case (fileutil/get-files (@state ::path)))))
        res (concat (map #(str "    [" (fileutil/filename %) "]") folders) (map #(str "    " (fileutil/filename %) "") files))]
    (if (and (@state ::selected) (> (count res) 0))
      (swap! state assoc ::hit (nth (concat folders files) (min (@state ::selected) (dec (count res)))))
      (swap! state assoc ::hit (fileutil/file (@state ::path) (@state ::search))))
    (editor/clear)
    (editor/insert (str (fileutil/absolute (@state ::path)) "\n"))
    (editor/insert (@state ::search))
    (editor/insert "\n-----------\n")
    (doseq [r res]
      (editor/insert (str r "\n")))
    (editor/beginning-of-buffer)
    (when (@state ::selected)
      (dotimes [n (+ (@state ::selected) 3)]
        (editor/forward-line))
      (editor/forward-char 2)
      (editor/delete 2)
      (editor/insert "#>"))
    (editor/beginning-of-buffer)
    (editor/forward-line)
    (editor/end-of-line)
  ))

(defn insert
  [st]
  (when (not (re-matches #"[/\\]" st)) ; Ignore forward and back slashes
    (swap! state update ::search #(str % st))
    (swap! state assoc ::selected 0)
    ;(swap! state update ::selected #(inc (or % -1)))
    (update-display)))

(defn delete
  []
  (when (> (count (@state ::search)) 0)
    (swap! state update ::search #(subs % 0 (dec (count (@state ::search)))))
    (swap! state assoc ::selected nil)
    (update-display)))

(defn up
  []
  (swap! state update ::path #(or (.getParent (io/file %)) %))
  (swap! state assoc ::search "")
  (swap! state assoc ::selected nil)
  (update-display))

(defn next-res
  []
  (swap! state update ::selected #(inc (or % -1)))
  (update-display))

(defn prev-res
  []
  (when (and (@state ::selected) (> (@state ::selected) 0))
    (swap! state update ::selected dec)
    (update-display)))

(defn execute
  [fun]
  (let [hit (@state ::hit)]
    (if (fileutil/folder? hit)
      (if (= hit "..")
        (up)
        (do 
          (swap! state assoc ::path hit
                             ::search ""
                             ::selected nil
                             ::hit nil)
          (update-display)))
      (fun hit))))

(defn execute-search
  [fun]
  (let [path (@state ::path)
        search (@state ::search)]
    (fun (fileutil/file (@state ::path) (@state ::search)))))
  
(defn keymap
  [fun]
  (merge
    {:cursor-color :green
     :space #(insert " ")
     :backspace delete
     :C-g editor/previous-buffer
     :esc editor/previous-buffer
     :C-j up
     :left up
     :C-k next-res
     :down next-res
     :tab prev-res ; tab and C-i are the same in terminal
     :up prev-res
     :enter #(execute fun)
     :M-enter #(execute-search fun)
    }
    (keys/alphanum-mapping insert)
    (keys/symbols-mapping insert)))

(defn run
  [fun]
  (let [context (editor/get-context)
        path (if (and (= (context :type) :file) (re-find #"/.*/" (context :value)))
                 (re-find #"/.*/" (context :value))
                 (editor/get-folder))]
    (editor/new-buffer "-findfile-")
    (editor/set-keymap (keymap fun))
    (reset-state path))
  (update-display))