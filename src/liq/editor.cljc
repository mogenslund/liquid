(ns liq.editor
  (:require [clojure.string :as str]
            ;#?(:cljs [lumo.io :as io :refer [slurp spit]])
            [liq.util :as util]
            [liq.highlighter :as highlighter]
            [liq.buffer :as buffer]))

(def state (atom {::commands {}
                  ::buffers {}
                  ::modes {}
                  ::new-buffer-hooks []
                  ::settings {:auto-switch-to-output true
                              :default-tabwidth 8}
                  ::exit-handler nil
                  ::window nil
                  ::output-handler nil}))

(def ^:private macro-seq (atom ())) ; Macrofunctionality might belong to input handler.
(def ^:private macro-record (atom false))

(defn get-window
  []
  (if-let [w (@state ::window)]
    w
    (let [d ((-> @state ::output-handler :dimensions))
          w {::buffer/top 1 ::buffer/left 1 ::buffer/rows (d :rows) ::buffer/cols (d :cols)}]
      (swap! state assoc ::window w)
      w)))

(defn set-setting
  "Add a key value pair to settings"
  [keyw value]
  (swap! state assoc-in [::settings keyw] value))

(defn get-setting
  "Get value given key from settings"
  [keyw]
  ((@state ::settings) keyw))

(defn set-output-handler
  "Sets \"device\" (map with output functions) for displaying
  buffers.
  See :help output-handler for more details."
  [output-handler]
  (swap! state assoc ::output-handler output-handler))

(defn set-exit-handler
  "Exit handler is the function to be called, when exit is called.
  This function usually depends on the environment Liquid is running
  in. Java or NodeJS for example."
  [exit-handler]
  (swap! state assoc ::exit-handler exit-handler))

(defn add-mode
  [keyw mode]
  (swap! state update ::modes assoc keyw mode))

(defn get-mode
  [keyw]
  (or ((@state ::modes) keyw) {}))

(defn add-new-buffer-hook
  "Add function: buffer -> buffer to new-buffer-hook"
  [fun]
  (swap! state update ::new-buffer-hooks conj (buffer/ensure-buffer-fun fun)))

(defn- deep-merge
  [m1 m2]
  (merge-with #(if (and (map? %1) (map? %2)) (deep-merge %1 %2) %1) m1 m2))

;; (add-key-bindings :fundamental-mode :normal {"-" #(message (rand-int 100))})
(defn add-key-bindings
  [major-mode mode keybindings]
  ;(swap! state update-in [::modes major-mode mode] #(merge-with merge % keybindings)) 
  (swap! state update-in [::modes major-mode mode] #(deep-merge keybindings %))) 

;; (set-command :mycommand #(message (rand-int 100)))
(defn set-command
  [keyw fun]
  (swap! state assoc-in [::commands keyw] fun))

(defn get-buffer-id-by-idx
  [idx]
  ((first (filter #(= (% ::idx) idx) (vals (@state ::buffers)))) ::id))

(defn get-buffer-id-by-name
  [name]
  (when-let [buf (first (filter #(= (% ::buffer/name) name) (vals (@state ::buffers))))]
    (buf ::id)))

(defn get-buffer
  [idname]
  (cond (nil? idname) (do)
        (number? idname) ((@state ::buffers) idname)
        true (get-buffer (get-buffer-id-by-name idname))))

(defn all-buffers
  []
  (reverse (sort-by ::idx (vals (@state ::buffers)))))

(defn regular-buffers
  []
  (filter #(not= (subs (str (% ::buffer/name) " ") 0 1) "*") (vals (@state ::buffers))))

(defn current-buffer-id
  "Highest idx is current buffer.
  So idx is updated each time buffer is switched."
  []
  (let [idxs (filter number? (map ::idx (vals (@state ::buffers))))]
    (when (not (empty? idxs))
      (get-buffer-id-by-idx (apply max idxs)))))

(comment
  (map ::idx (vals (@state ::buffers)))
  (current-buffer-id))

(defn current-buffer
  []
  (get-buffer (current-buffer-id)))

(defn switch-to-buffer
  [idname]
  (if (number? idname)
    (do
      (swap! state assoc-in [::buffers idname ::idx] (util/counter-next))
      idname)
    (switch-to-buffer (get-buffer-id-by-name idname)))) 

(defn previous-buffer
  "n = 1 means previous"
  ([n]
   (let [idx (first (drop n (reverse (sort (map ::idx (vals (@state ::buffers)))))))]
     (when idx
       (switch-to-buffer (get-buffer-id-by-idx idx)))))
  ([] (previous-buffer 1)))

(defn previous-regular-buffer-id
  "n = 1 means previous"
  ([n]
   (let [idx (first (drop n (reverse (sort (map ::idx (vals (@state ::buffers)))))))]
     (when idx
       (get-buffer-id-by-idx idx))))
  ([] (previous-regular-buffer-id 1)))

(defn previous-regular-buffer
  "n = 1 means previous"
  ([n]
   (let [idx (first (drop n (reverse (sort (map ::idx (regular-buffers))))))]
     (when idx
       (switch-to-buffer (get-buffer-id-by-idx idx)))))
  ([] (previous-regular-buffer 1)))

(defn oldest-buffer
  []
  (let [idx (first (sort (map ::idx (regular-buffers))))]
    (when idx
      (switch-to-buffer (get-buffer-id-by-idx idx)))))

(defn apply-to-buffer
  "Apply function to buffer"
  ([idname fun]
   (if (number? idname)
     (swap! state update-in [::buffers idname] (buffer/ensure-buffer-fun fun))
     (apply-to-buffer (get-buffer-id-by-name idname) fun)))
  ([fun] (apply-to-buffer (current-buffer-id) fun)))

(comment
  (new-buffer))
  
(defn apply-to-all-buffers
  [f]
  (swap! state update ::buffers
         (fn [m] (into {} (for [[k buf] m] [k (f buf)])))))

(defn highlight-paren
  [buf]
  (if ({\( \) \) \( \{ \} \} \{ \[ \] \] \[} (buffer/get-char buf))
    (let [r (buffer/paren-matching-region buf (buf ::buffer/cursor))
          p (second r)]
      (if p
        (buffer/set-style buf p :red) 
        buf))
    buf))

(defn paint-buffer-old
  ([buf]
   (when (@state ::output-handler)
     (apply-to-buffer "*status-line*"
       #(-> %
            buffer/clear
            (buffer/insert-string
              (str (or (buf ::buffer/filename) (buf ::buffer/name)) "  "
                   (if (buffer/dirty? buf) " [+] " "     ")
                   (cond (= (buf ::buffer/mode) :insert) "-- INSERT --   "
                         (= (buf ::buffer/mode) :visual) "-- VISUAL --   "
                         true "               ")
                   (-> buf ::buffer/cursor ::buffer/row) "," (-> buf ::buffer/cursor ::buffer/col)))
            buffer/beginning-of-buffer))
     ;((@state ::output-handler) (get-buffer "*status-line*"))
     ((-> @state ::output-handler :printer) (assoc (highlight-paren buf) :status-line (get-buffer "*status-line*")))))
  ([] (paint-buffer-old (current-buffer))))

(defn paint-buffer
  ([nameid]
   (when (@state ::output-handler)
     (apply-to-buffer nameid buffer/update-tow)
     (let [buf (get-buffer nameid)]
       (apply-to-buffer "*status-line*"
         #(-> %
              buffer/clear
              (buffer/insert-string
                (str (or (buf ::buffer/filename) (buf ::buffer/name)) "  "
                     (if (buffer/dirty? buf) " [+] " "     ")
                     (cond (= (buf ::buffer/mode) :insert) "-- INSERT --   "
                           (= (buf ::buffer/mode) :visual) "-- VISUAL --   "
                           true "               ")
                     (-> buf ::buffer/cursor ::buffer/row) "," (-> buf ::buffer/cursor ::buffer/col)))
              buffer/beginning-of-buffer
              buffer/update-tow))
       ;((@state ::output-handler) (get-buffer "*status-line*"))
       ((-> @state ::output-handler :printer) (assoc (highlight-paren buf) :status-line (get-buffer "*status-line*"))))))
  ([] (paint-buffer (current-buffer-id))))

(defn paint-all-buffers
  []
  (doseq [buf (sort-by ::idx (vals (@state ::buffers)))]
    (paint-buffer (buf ::id))))

(comment (map ::buffer/name (sort-by ::idx (vals (@state ::buffers)))))
(comment (paint-buffer "*delimeter*"))

(defn invalidate-ui
  []
  (when-let [f (-> @state ::output-handler :invalidate)]
    (f)))

(defn paint-all-buffer-groups
  []
  (invalidate-ui)
  (doseq [buf (sort-by ::idx (map #(first (sort-by ::idx %)) (vals (group-by #(-> % ::buffer/window ::buffer/group) (vals (@state ::buffers))))))]
    (paint-buffer (buf ::id))))
       
; (group-by ::buffer/group (vals (@state ::buffers)))
; (map ::id (sort-by ::idx (map #(first (sort-by ::idx %)) (group-by #(-> % ::buffer/window ::buffer/group) (vals (@state ::buffers))))))


(defn message
  [s & {:keys [:append :view :timer]}]
  (if append
    ;(apply-to-buffer "*output*" #(-> % (buffer/append-buffer (buffer/buffer (str s "\n"))) buffer/end-of-buffer))
    (apply-to-buffer "*output*" #(-> % buffer/end-of-buffer (buffer/insert-string (str s "\n")) buffer/end-of-buffer))
    (apply-to-buffer "*output*" #(-> % buffer/clear (buffer/insert-string (str s)))))
  (paint-buffer "*output*")
  (when (and view (get-setting :auto-switch-to-output))
    (switch-to-buffer "*output*")
    (when timer (future (Thread/sleep timer) (previous-buffer) (paint-buffer))))
  (paint-buffer))

(defn force-kill-buffer
  ([idname]
   (when (not= idname "scratch")
     (let [id (if (number? idname) idname (get-buffer-id-by-name idname))]
       (swap! state update ::buffers dissoc id))
     (previous-regular-buffer 0)))
  ([] (force-kill-buffer (current-buffer-id))))

(defn kill-buffer
  ([idname]
   (if (not (buffer/dirty? (get-buffer idname)))
     (force-kill-buffer idname)
     (message "There are unsaved changes. Use bd! to force kill." :view true :timer 1500)))
  ([] (kill-buffer (current-buffer-id))))

(defn highlight-buffer
  ([idname]
   (apply-to-buffer idname
     (fn [buf]
       (let [hl (first (filter identity (map #(-> % get-mode :syntax) (buf ::buffer/major-modes))))]
         (if hl
           (highlighter/highlight buf hl)
           buf)))))
  ([] (highlight-buffer (current-buffer-id))))

(defn highlight-buffer-row
  ([idname]
   (apply-to-buffer idname
     (fn [buf]
       (let [hl (first (filter identity (map #(-> % get-mode :syntax) (buf ::buffer/major-modes))))]
         (if hl
           (highlighter/highlight buf hl (-> buf ::buffer/cursor ::buffer/row))
           buf)))))
  ([] (highlight-buffer-row (current-buffer-id))))

(defn new-buffer
  ([text {:keys [name] :as options}]
   (let [id (util/counter-next)
         o (if (options :rows)
             (merge {:group (util/counter-next)} options)
             (let [b (current-buffer) ;; TODO If there is no current-buffer, there will be a problem!
                   w (b ::buffer/window)] 
               (assoc options :top (w ::buffer/top)
                              :left (w ::buffer/left)
                              :rows (w ::buffer/rows)
                              :cols (w ::buffer/cols)
                              :group (or (w ::buffer/group) (util/counter-next))
                              :bottom-border (w ::buffer/bottom-border)))) 
         buf (reduce #(%2 %1)
                     (assoc (buffer/buffer text o) ::id id ::idx id ::buffer/tabwidth (get-setting :default-tabwidth))
                     (@state ::new-buffer-hooks))]
     (swap! state update ::buffers assoc id buf) 
     (highlight-buffer id)
     (switch-to-buffer id)
     (paint-buffer)))
  ([text] (new-buffer text {})))

(defn open-file
  [path]
  (let [p (util/resolve-home path)]
    (if (get-buffer-id-by-name p)
      (switch-to-buffer p)
      (new-buffer (or (util/read-file p) "") {:name p :filename p}))))

(defn dirty-buffers
  []
  (filter buffer/dirty? (all-buffers)))

(defn force-exit-program
  []
  ((@state ::exit-handler)))

(defn exit-program
  []
  (let [bufs (dirty-buffers)]
    (if (empty? bufs)
      (force-exit-program)
      (do
        (message (str
                  "There are unsaved files. Use :q! to force quit:\n"
                  (str/join "\n" (map ::buffer/filename bufs))) :view true :timer 1500)))))

(def tmp-keymap (atom nil))

(defn merge-mode-maps
  [major-modes mode]
  ;(apply deep-merge (reverse (map #((get-mode %) mode) major-modes)))
  (apply merge-with
          #(if (and (map? %1) (map? %2)) (deep-merge %1 %2) %1)
          (map #((get-mode %) mode) major-modes)))

(defn get-mode-fun
  [major-modes mode c]
  ((merge-mode-maps major-modes mode) c))

(defn resolve-command
  [command]
  (let [m (or (meta command) {})
        n (@state ::repeat-counter)]
    (cond (and (m :buffer) (not (some #{2} (map count (m :arglists))))) #(apply-to-buffer command)
          (m :buffer) #(apply-to-buffer (fn [buf] (command buf (max 1 n))))
          true command)))
  

(defn handle-keyword-action
  [k]
  (when-let [command (-> @state ::commands k)]
    (let [m (or (meta command) {})
          n (@state ::repeat-counter)
          action (resolve-command command)]
      (action)
      (swap! state assoc ::repeat-counter 0
                         ::last-action action)))
  nil)

(defn handle-input
  [c]
  ;(spit "/tmp/liq.log" (str "INPUT: " c "\n"))
  (when (and @macro-record (not= c "Q"))
    (swap! macro-seq conj c))
  (when-let [buf (current-buffer)]
    (if (and (not= (buf ::buffer/mode) :insert)
             (not (and @tmp-keymap (@tmp-keymap :selfinsert)))
             (contains? #{"1" "2" "3" "4" "5" "6" "7" "8" "9" "0"} c)
             (not (and (= c "0") (= (@state ::repeat-counter) 0))))
      (do (swap! state update ::repeat-counter (fn [t] (+ (* 10 t) (Integer/parseInt c))))
          nil)
      (let [mode (buf ::buffer/mode)
            major-modes (buf ::buffer/major-modes)
            tmp-k-selfinsert (and @tmp-keymap (@tmp-keymap :selfinsert)) 
            tmp-k (and @tmp-keymap
                       (or (@tmp-keymap c)
                           (and tmp-k-selfinsert
                                (fn [] (apply-to-buffer #(tmp-k-selfinsert % c))))))
            _ (reset! tmp-keymap nil)
            action (or
                     tmp-k
                     (and (fn? ((get-mode (first major-modes)) mode))
                          (((get-mode (first major-modes)) mode) c))
                     (get-mode-fun major-modes mode c)
                     (when (not= mode :insert)
                       (get-mode-fun major-modes :normal c)))]
                     
        (swap! state assoc ::skip-number false)
        (cond (= action ::last-action) (when (@state ::last-action) ((@state ::last-action))) 
              (or (fn? action) (var? action)) (do (swap! state assoc ::last-action action) ((resolve-command action)))
              (keyword? action) (handle-keyword-action action)
              (map? action) (do (reset! tmp-keymap action) (when (action :description) (message (action :description))))
              ;action (swap! state update-in [::buffers (current-buffer-id)] (action :function))
              (= mode :insert) (apply-to-buffer #(buffer/insert-char % (first c))))
        (cond (= c "esc") (highlight-buffer) ; TODO Maybe not highlight from scratch each time
              (= mode :insert) (highlight-buffer-row))
        (paint-buffer)))))

(defn record-macro
  []
  (if (not @macro-record)
    (do
      (message "Recording macro")
      (reset! macro-seq ()))
    (message "Recording finished"))
  (swap! macro-record not))

(defn run-macro
  []
  (when (not @macro-record)
    (doall (map handle-input (reverse @macro-seq)))))
