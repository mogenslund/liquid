(ns liq.commands
  (:require [clojure.string :as str]
            [liq.editor :as editor :refer [apply-to-buffer switch-to-buffer get-buffer]]
            [liq.buffer :as buffer :refer [delete-region shrink-region set-insert-mode]]
            [liq.window-manager :as window-manager]
            [clojure.pprint :as pprint]
            #?(:clj [liq.tools.shell :as s])
            #?(:cljs [cljs.js :refer [eval eval-str empty-state]])
            [liq.util :as util]))

(swap! editor/state assoc ::editor/repeat-counter 0)

(defn repeat-fun
  [fun args]
  (let [r (max (min (@editor/state ::editor/repeat-counter) 999) 1)
        n (when (not (empty? args)) (util/int-value (first args)))]
    (when (not= (@editor/state ::editor/repeat-counter) 0) (swap! editor/state assoc ::editor/repeat-counter 0))
    (editor/apply-to-buffer #(fun % (or n r)))))

(defn non-repeat-fun
  [fun]
  (when (not= (@editor/state ::editor/repeat-counter) 0) (swap! editor/state assoc ::editor/repeat-counter 0))
  (editor/apply-to-buffer fun))

(defn write-file
  "Save buffer to associated file"
  []
  (let [buf (editor/current-buffer)]
    (when-let [f (buf ::buffer/filename)]
      (util/write-file f (buffer/text buf)))
    (apply-to-buffer #(buffer/set-dirty % false))))

(defn external-command
  [text]
  #?(:clj (let [f (or (editor/current-file) ".")
                folder (editor/current-folder)
                text1 (str/replace text #"%" f)]
            (editor/message (str "Running command: " text1 "\n") :view true)
            (future
              (doseq [output (s/cmdseq folder "/bin/sh" "-c" text1)]
                (editor/message output :append true)))))
   #?(:cljs (do)))

(defn e-cmd
  [& args]
  (let [t (first args)]
    (cond (or (= t ".") (not t)) (((editor/get-mode :dired-mode) :init))
          (util/folder? t) (((editor/get-mode :dired-mode) :init) t)
          true (editor/open-file t))))

(defn open-file-at-point
  []
  (when (not= (@editor/state ::editor/repeat-counter) 0) (swap! editor/state assoc ::editor/repeat-counter 0))
  (let [buf (editor/current-buffer)
        w (str/replace (buffer/word buf) #"\"" "")
        project-part (when-let [p (re-find #"^:\+(?:\w+|-)+$" w)] (str (subs p 2) ".md"))
        part (or project-part (re-find #"[^:\(\)\[\]\{\}]+" w))
        buffer-file (or (buf ::buffer/filename)
                        ((editor/get-buffer (editor/previous-regular-buffer-id)) ::buffer/filename))
        alternative-parent (if buffer-file
                             (util/get-folder buffer-file)
                             (or ((editor/get-buffer (editor/previous-regular-buffer-id)) :liq.modes.dired-mode/folder) "."))
        filepath (util/resolve-path part alternative-parent)]
    (e-cmd filepath)))

(defn copy-selection-to-clipboard
  [buf]
  (let [p (buffer/get-selection buf)
        text (buffer/get-selected-text buf)]
    (if p
      (do
        (util/set-clipboard-content text false)
        (-> buf
            buffer/set-normal-mode
            (assoc ::buffer/cursor p)
            buffer/update-mem-col))
      buf)))

(defn paste-clipboard
  []
  (when (not= (@editor/state ::editor/repeat-counter) 0) (swap! editor/state assoc ::editor/repeat-counter 0))
  (let [text (util/clipboard-content)] 
    (if (util/clipboard-line?)
      (apply-to-buffer
        #(-> %
             buffer/append-line
             ;buffer/end-of-line
             (buffer/insert-string text)
             buffer/beginning-of-line
             buffer/set-normal-mode))
      (apply-to-buffer
        #(-> %
             buffer/set-insert-mode
             buffer/right
             (buffer/insert-string text)
             (buffer/right (dec (count text)))
             buffer/set-normal-mode)))))

(defn paste-clipboard-here
  []
  (when (not= (@editor/state ::editor/repeat-counter) 0) (swap! editor/state assoc ::editor/repeat-counter 0))
  (let [text (util/clipboard-content)] 
    (if (util/clipboard-line?)
      (apply-to-buffer
        #(-> %
             buffer/beginning-of-line
             (buffer/insert-string (str text "\n"))
             buffer/up
             buffer/beginning-of-line
             buffer/set-normal-mode))
      (apply-to-buffer
        #(-> %
             buffer/set-insert-mode
             (buffer/insert-string text)
             (buffer/right (dec (count text)))
             buffer/set-normal-mode)))))

(defn ^:buffer delete-line
  [buf]
  (let [text (buffer/line buf)]
    (util/set-clipboard-content text true)
    (-> buf
        buffer/set-undo-point
        buffer/delete-line)))

(defn copy-line
  []
  (let [text (buffer/line (editor/current-buffer))]
    (util/set-clipboard-content text true)))

(defn delete
  [buf]
  (let [text (buffer/get-selected-text buf)]
    (util/set-clipboard-content text false)
    (buffer/delete buf)))

(defn cut-region
  [buf r]
  (if r
    (let [text (buffer/text buf r)]
      (util/set-clipboard-content text false)
      (buffer/delete-region buf r))
    buf))

(defn yank-region
  [buf r]
  (if r
    (let [text (buffer/text buf r)]
      (util/set-clipboard-content text false)
      (assoc buf ::buffer/cursor (first r)))
    buf))

(defn yank-filename
  []
  (when-let [f ((editor/current-buffer) ::buffer/filename)]
    (util/set-clipboard-content f false)))

(defn typeahead-defs
  [buf]
  (let [headlines (filter #(re-find #"^\(def|^#" (second %))
                          (map #(vector % (buffer/line buf %))
                               (range 1 (inc (buffer/line-count buf)))))]
    (((editor/get-mode :typeahead-mode) :init) headlines 
                                               second
                                               (fn [res]
                                                 (apply-to-buffer #(-> %
                                                                       (assoc ::buffer/cursor {::buffer/row (first res) ::buffer/col 1})
                                                                       (assoc ::buffer/tow {::buffer/row (first res) ::buffer/col 1})))))))

(defn typeahead-lines
  [buf]
  (let [lines (map #(vector % (buffer/line buf %))
                   (range 1 (inc (buffer/line-count buf))))]
    (((editor/get-mode :typeahead-mode) :init) lines 
                                               second
                                               (fn [res]
                                                 (apply-to-buffer #(-> %
                                                                       (assoc ::buffer/cursor {::buffer/row (first res) ::buffer/col 1})
                                                                       (assoc ::buffer/tow {::buffer/row (first res) ::buffer/col 1})))))))

(defn get-namespace
  [buf]
  (let [content (buffer/line buf 1)]
    (re-find #"(?<=\(ns )[-a-z0-9\\.]+" content))) ;)


(defn raw-eval-sexp-at-point
  [buf]
  (when (not= (@editor/state ::editor/repeat-counter) 0) (swap! editor/state assoc ::editor/repeat-counter 0))
  (let [sexp (if (= (buf ::buffer/mode) :visual) (buffer/get-selected-text buf) (buffer/sexp-at-point buf))
        namespace (or (get-namespace buf) "user")]
    (try
      (load-string
        (str "(do (ns " namespace ") (in-ns '" namespace ") " sexp ")"))
      (catch Exception e (println (util/pretty-exception e))))))

(defn sanitize-output
  [x]
  (cond (string? x) x
        (and (map? x) (x ::editor/buffers)) "<editor/state>"
        true (pr-str x)))

(defn eval-buffer
  [buf]
  #?(:clj
      (do
        (when (not= (@editor/state ::editor/repeat-counter) 0) (swap! editor/state assoc ::editor/repeat-counter 0))
        (let [text (buffer/text buf)]
          (binding [*print-length* 200]
            (editor/message "" :view true); ( :view true :timer 1500)
            (future
              (with-redefs [println (fn [& args] (editor/message (str/join " " args) :append true))]
                (try
                  (println (sanitize-output (load-string text)))
                  (catch Exception e (println (util/pretty-exception e)))))))))
     :cljs (do
             (when (not= (@editor/state ::editor/repeat-counter) 0) (swap! editor/state assoc ::editor/repeat-counter 0))
             (let [code (buffer/text buf)]
               (binding [cljs.js/*eval-fn* cljs.js/js-eval]
                 (eval-str (cljs.js/empty-state) code "bla"
                           {:eval cljs/js-eval :context :statements}
                           #(editor/message (str %) :view true)))))))

(defn eval-sexp-at-point
  [buf]
  #?(:bb (do)
     :clj
      (do
        (when (not= (@editor/state ::editor/repeat-counter) 0) (swap! editor/state assoc ::editor/repeat-counter 0))
        (let [sexp (if (= (buf ::buffer/mode) :visual) (buffer/get-selected-text buf) (buffer/sexp-at-point buf))
              namespace (or (get-namespace buf) "user")]
          (create-ns (symbol namespace))
          (binding [*print-length* 200
                    *ns* (find-ns (symbol namespace))]
            (editor/message "" :view true); ( :view true :timer 1500)
            (future
              (with-redefs [println (fn [& args] (editor/message (str/join " " args) :append true))]
                (try
                  (println (sanitize-output
                             (load-string
                               (str
                                 "(do (ns " namespace ") (in-ns '"
                                 namespace
                                 ") " sexp "\n)"))))
                  (catch Exception e (println (util/pretty-exception e)))))))))
     :cljs (do
             (when (not= (@editor/state ::editor/repeat-counter) 0) (swap! editor/state assoc ::editor/repeat-counter 0))
             (let [sexp (if (= (buf ::buffer/mode) :visual) (buffer/get-selected-text buf) (buffer/sexp-at-point buf))
                   namespace (or (get-namespace buf) "user")]
               (binding [cljs.js/*eval-fn* cljs.js/js-eval]
                 (eval-str (cljs.js/empty-state) sexp "bla"
                           {:eval cljs/js-eval :context :statements}
                           #(editor/message (str %) :view true)))))))


(defn raw-eval-sexp-at-point
  [buf]
  (when (not= (@editor/state ::editor/repeat-counter) 0) (swap! editor/state assoc ::editor/repeat-counter 0))
  (let [sexp (if (= (buf ::buffer/mode) :visual) (buffer/get-selected-text buf) (buffer/sexp-at-point buf))
        namespace (or (get-namespace buf) "user")]
    (try
      (load-string
        (str "(do (ns " namespace ") (in-ns '" namespace ") " sexp ")"))
      (catch Exception e (println (util/pretty-exception e))))))


(defn evaluate-file-raw
  "Evaluate a given file raw, without using
  with-out-str or other injected functionality.
  If no filepath is supplied the path connected
  to the current buffer will be used."
  ([filepath]
   (try (editor/message (load-file filepath))
     (catch Exception e (editor/message (util/pretty-exception e)))))
  ([] (when-let [filepath ((editor/current-buffer) ::buffer/filename)] (evaluate-file-raw filepath))))

(defn run-command
  [s]
  (let [fargs (str/split s #" ")
        keyw (keyword (first fargs))
        args (rest fargs)]
    (when-let [fun (-> @editor/state ::editor/commands keyw)]
      (apply fun args)))) 

(defn output-snapshot
  [] 
  (let [id (editor/get-buffer-id-by-name "output-snapshot")]
    (if (= id (editor/current-buffer-id))
      (editor/previous-buffer)
      (do (if id
            (editor/switch-to-buffer id)
            (editor/new-buffer "" {:name "output-snapshot"}))
          (editor/apply-to-buffer
            #(-> %
                 buffer/clear
                 (buffer/insert-string
                   (buffer/text (editor/get-buffer "*output*")))))))))

(defn yank-inner-word
  []
  (non-repeat-fun #(->> % buffer/word-region (yank-region %))))

(defn yank-inner-paren
  []
  (non-repeat-fun #(->> % buffer/paren-region (shrink-region %) (yank-region %))))

(defn load-commands
  []
  (swap! editor/state update ::editor/commands merge 
    {:left #'buffer/left
     :down #'buffer/down
     :up #'buffer/up
     :right #'buffer/right
     :first-non-blank #(non-repeat-fun buffer/first-non-blank)
     :0 #(non-repeat-fun buffer/beginning-of-line)
     :beginning-of-line #(non-repeat-fun buffer/beginning-of-line)
     :move-matching-paren #(non-repeat-fun buffer/move-matching-paren)
     :word-forward #'buffer/word-forward
     :word-forward-ws #'buffer/word-forward-ws
     :beginning-of-word #'buffer/beginning-of-word
     :end-of-word #'buffer/end-of-word
     :end-of-word-ws #'buffer/end-of-word-ws
     :end-of-line #'buffer/end-of-line
    ;:delete-char (fn [& args] (repeat-fun buffer/delete-char args))
     :delete-char (fn [& args] (repeat-fun #(do (util/set-clipboard-content (str (buffer/get-char %1)) false) (buffer/delete-char %1 %2)) args))
     :copy-selection-to-clipboard (with-meta #(apply-to-buffer copy-selection-to-clipboard) {:doc "Copy selection to clipboard"})
     :copy-line #'copy-line
     :yank-filename #'yank-filename
     :yank-inner-word #'yank-inner-word
     :yank-inner-paren #'yank-inner-paren
     :yank-inner-bracket (fn [] (non-repeat-fun #(->> % buffer/bracket-region (shrink-region %) (yank-region %))))
     :yank-inner-brace (fn [] (non-repeat-fun #(->> % buffer/brace-region (shrink-region %) (yank-region %))))
     :yank-inner-quote (fn [] (non-repeat-fun #(->> % buffer/quote-region (shrink-region %) (yank-region %))))
     :yank-outer-paren (fn [] (non-repeat-fun #(->> % buffer/paren-region (yank-region %))))
     :yank-outer-bracket (fn [] (non-repeat-fun #(->> % buffer/bracket-region (yank-region %))))
     :yank-outer-brace (fn [] (non-repeat-fun #(->> % buffer/brace-region (yank-region %))))
     :yank-outer-quote (fn [] (non-repeat-fun #(->> % buffer/quote-region (yank-region %))))

     :delete #(apply-to-buffer delete)
     :delete-line #'delete-line
     :delete-inner-word (fn [] (non-repeat-fun #(->> % buffer/word-region (cut-region %))))
     :delete-inner-paren (fn [] (non-repeat-fun #(->> % buffer/paren-region (shrink-region %) (cut-region %))))
     :delete-inner-bracket (fn [] (non-repeat-fun #(->> % buffer/bracket-region (shrink-region %) (cut-region %))))
     :delete-inner-brace (fn [] (non-repeat-fun #(->> % buffer/brace-region (shrink-region %) (cut-region %))))
     :delete-inner-quote (fn [] (non-repeat-fun #(->> % buffer/quote-region (shrink-region %) (cut-region %))))
     :delete-outer-paren (fn [] (non-repeat-fun #(->> % buffer/paren-region  (cut-region %))))
     :delete-outer-bracket  (fn [] (non-repeat-fun #(->> % buffer/bracket-region  (cut-region %))))
     :delete-outer-brace (fn [] (non-repeat-fun #(->> % buffer/brace-region  (cut-region %))))
     :delete-outer-quote (fn [] (non-repeat-fun #(->> % buffer/quote-region  (cut-region %))))
     :delete-to-word (fn [& args] (repeat-fun #(cut-region %1
                                                [(%1 ::buffer/cursor)
                                                 ((buffer/left (buffer/word-forward %1 %2)) ::buffer/cursor)]) args))
     :delete-to-end-of-word (fn [& args] (repeat-fun #(cut-region %1 (buffer/end-of-word-region %1 %2)) args))
     :delete-to-end-of-word-ws (fn [& args] (repeat-fun #(cut-region %1 (buffer/end-of-word-ws-region %1 %2)) args))

     :change (fn [] (non-repeat-fun #(->> % delete set-insert-mode)))
     :change-inner-word (fn [] (non-repeat-fun #(->> % buffer/word-region (cut-region %) set-insert-mode)))
     :change-inner-paren (fn [] (non-repeat-fun #(->> % buffer/paren-region (shrink-region %) (cut-region %) set-insert-mode)))
     :change-inner-bracket (fn [] (non-repeat-fun #(->> % buffer/bracket-region (shrink-region %) (cut-region %) set-insert-mode)))
     :change-inner-brace (fn [] (non-repeat-fun #(->> % buffer/brace-region (shrink-region %) (cut-region %) set-insert-mode)))
     :change-inner-quote (fn [] (non-repeat-fun #(->> % buffer/quote-region (shrink-region %) (cut-region %) set-insert-mode)))
     :change-outer-paren (fn [] (non-repeat-fun #(->> % buffer/paren-region (cut-region %) set-insert-mode)))
     :change-outer-bracket (fn [] (non-repeat-fun #(->> % buffer/bracket-region (cut-region %) set-insert-mode)))
     :change-outer-brace (fn [] (non-repeat-fun #(->> % buffer/brace-region (cut-region %) set-insert-mode)))
     :change-outer-quote (fn [] (non-repeat-fun #(->> % buffer/quote-region (cut-region %) set-insert-mode)))
     :change-line (fn [] (non-repeat-fun #(->> % buffer/line-region (cut-region %) set-insert-mode)))
     :change-eol (fn [] (non-repeat-fun #(->> % buffer/eol-region (cut-region %) set-insert-mode)))
     :change-to-end-of-word (fn [& args] (repeat-fun #(set-insert-mode (cut-region %1 (buffer/end-of-word-region %1 %2))) args))
     :change-to-end-of-word-ws (fn [& args] (repeat-fun #(set-insert-mode (cut-region %1 (buffer/end-of-word-ws-region %1 %2))) args))

     :select-inner-word (fn [] (non-repeat-fun #(buffer/expand-selection % (buffer/word-region %))))
     :select-inner-paren (fn [] (non-repeat-fun #(buffer/expand-selection % (shrink-region % (buffer/paren-region %)))))
     :select-inner-bracket (fn [] (non-repeat-fun #(buffer/expand-selection % (shrink-region % (buffer/bracket-region %)))))
     :select-inner-brace (fn [] (non-repeat-fun #(buffer/expand-selection % (shrink-region % (buffer/brace-region %)))))
     :select-inner-quote (fn [] (non-repeat-fun #(buffer/expand-selection % (shrink-region % (buffer/quote-region %)))))
     :select-outer-paren (fn [] (non-repeat-fun #(buffer/expand-selection % (buffer/paren-region %))))
     :select-outer-bracket (fn [] (non-repeat-fun #(buffer/expand-selection % (buffer/bracket-region %))))
     :select-outer-brace (fn [] (non-repeat-fun #(buffer/expand-selection % (buffer/brace-region %))))
     :select-outer-quote (fn [] (non-repeat-fun #(buffer/expand-selection % (buffer/quote-region %))))
     :select-line (fn [] (non-repeat-fun #(buffer/expand-selection % (buffer/line-region %))))

     :insert-at-line-end #'buffer/insert-at-line-end
     :insert-at-beginning-of-line #'buffer/insert-at-beginning-of-line
     :delete-to-line-end #'buffer/delete-to-line-end

     :append-line (fn [] (non-repeat-fun #(buffer/append-line %)))
     :append-line-above #(non-repeat-fun (fn [buf] (-> buf buffer/beginning-of-line (buffer/insert-char \newline) buffer/up)))
     :join-lines #'buffer/join-lines
     :join-lines-space #'buffer/join-lines-space

     :eval #(eval-sexp-at-point (editor/current-buffer))
     :eval-sexp-at-point #(eval-sexp-at-point (editor/current-buffer))
     :raw-eval-sexp-at-point #(raw-eval-sexp-at-point (editor/current-buffer))
     :evaluate-file-raw evaluate-file-raw
     :eval-buffer #(eval-buffer (editor/current-buffer))

     :paste-clipboard paste-clipboard
     :paste-clipboard-here paste-clipboard-here

     :beginning-of-buffer #'buffer/beginning-of-buffer
     :navigate-definitions #(typeahead-defs (editor/current-buffer))
     :navigate-lines #(typeahead-lines (editor/current-buffer))
     :open-file-at-point open-file-at-point
     :end-of-buffer #'buffer/end-of-buffer
     :scroll-cursor-top #'buffer/scroll-cursor-top
     :scroll-page #'buffer/page-down
     :page-down #'buffer/page-down
     :page-up #'buffer/page-up

     :set-visual-mode #'buffer/set-visual-mode
     :set-normal-mode #'buffer/set-normal-mode
     :set-insert-mode #'buffer/set-insert-mode
     :insert-after-point (fn [] (non-repeat-fun #(-> % buffer/set-insert-mode buffer/right)))
     :search  #(non-repeat-fun buffer/search)
     :undo #'buffer/undo
     :q #'editor/exit-program
     :q! #'editor/force-exit-program
     :bnext #'editor/oldest-buffer
     :bn #'editor/oldest-buffer
     :new #(editor/new-buffer "" {})
     :reload-file #(editor/reload-file)
     :buffers #(((editor/get-mode :buffer-chooser-mode) :init))
     :buffer-major-modes #(editor/message (str ((editor/current-buffer) ::buffer/major-modes)))
     :buffer-info #(editor/message (with-out-str (pprint/pprint (buffer/info (editor/current-buffer)))))
     :paint-all-buffers #'editor/paint-all-buffers
     :paint-all-buffer-groups #'editor/paint-all-buffer-groups
     :settings #(editor/message (@editor/state ::editor/settings))
     :window-detach #'window-manager/window-detach
     :window-smaller #(window-manager/window-resize-vertical -1)
     :window-larger #(window-manager/window-resize-vertical 1)
     :window-narrower #(window-manager/window-resize-horizontal -1)
     :window-wider #(window-manager/window-resize-horizontal 1)
     :window-below #'window-manager/window-below
     :window-above #'window-manager/window-above
     :window-right #'window-manager/window-right
     :window-left #'window-manager/window-left
     :window-arrange-mode #'window-manager/init
     :window-set (fn [sleft sright stop sbottom] (window-manager/window-set (Double/parseDouble sleft)
                                                                            (Double/parseDouble sright)
                                                                            (Double/parseDouble stop)
                                                                            (Double/parseDouble sbottom)))
     :ls #(((editor/get-mode :buffer-chooser-mode) :init))
     :previous-regular-buffer editor/previous-regular-buffer
     :help (fn [& args] (apply ((editor/get-mode :help-mode) :init) args))
     :w #'write-file
     :wq #(do (write-file) (editor/exit-program))
     :t #(editor/open-file "/home/sosdamgx/proj/liquid/src/dk/salza/liq/slider.clj")
     :bd #(editor/kill-buffer)
     :bd! #(editor/force-kill-buffer)
     :t1 #(editor/highlight-buffer)
     :ts #(editor/message (buffer/sexp-at-point (editor/current-buffer)))
     :t2 #(editor/message (buffer/word (editor/current-buffer)))
     :t3 #(((editor/get-mode :typeahead-mode) :init
               ["aaa" "bbb" "aabb" "ccc"]
               str
               (fn [res]
                 (editor/previous-buffer)
                 (editor/apply-to-buffer (fn [buf] (buffer/insert-string buf res))))))
     :t4 #(editor/message (pr-str (buffer/line (editor/current-buffer) 1)))
     :t5 #(editor/message (pr-str (:liq.buffer/lines (editor/current-buffer))))
     :t6 #(((editor/get-mode :info-dialog-mode) :init) "This is the info dialog")
     :output-snapshot output-snapshot 
     :! (fn [& args] (external-command (str/join " " args)))
     :git (fn [& args] (external-command (str "git " (str/join " " args))))
     :grep (fn [& args] (external-command (str "grep " (str/join " " args))))
     :man (fn [& args] (external-command (str "man " (str/join " " args))))
     :tree (fn [& args] (external-command (str "tree " (str/join " " args))))
     :locate (fn [& args] (external-command (str "locate " (str/join " " args))))
     :node (fn [& args] (external-command (str "node " (if (empty? args) "%" (str/join " " args)))))
     :python (fn [& args] (external-command (str "python " (if (empty? args) "%" (str/join " " args)))))
     :bb (fn [& args] (external-command (str "bb " (if (empty? args) "%" (str/join " " args)))))
     :e e-cmd
     :Ex (fn [] (e-cmd "."))
     :testc ^:buffer (fn [buf] (future (Thread/sleep 1000) (editor/message "Test C: With inline buffer meta")) (buffer/down buf))
     :testd (with-meta (fn [buf] (future (Thread/sleep 1000) (editor/message "Test D: Using with-meta inline"))
                                 (buffer/down buf)) {:buffer true :doc "Some documentation for Test C"})
     :edit e-cmd}))
 

