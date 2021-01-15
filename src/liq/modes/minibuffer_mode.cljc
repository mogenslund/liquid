(ns liq.modes.minibuffer-mode
  (:require [clojure.string :as str]
            [liq.util :as util]
            #?(:clj [liq.tools.shell :as s])
            [liq.editor :as editor :refer [apply-to-buffer switch-to-buffer get-buffer]]
            [liq.buffer :as buffer]))

(def state (atom {}))

(defn commands-completion
  []
  (let [buf (editor/current-buffer)
        w (-> buf buffer/left buffer/word)]
    (when (re-matches #":.*" w)
      (((editor/get-mode :typeahead-mode) :init)
       (filter #(re-find (re-pattern (str "^" w)) %) (map str (sort (keys (@editor/state ::editor/commands)))))
       str
       (fn [hit] (editor/apply-to-buffer
                   #(-> (buffer/insert-string % (str (subs hit (count w)) " "))
                        buffer/end-of-buffer
                        buffer/set-insert-mode
                        buffer/right)))
       :search w
       :position :relative))))

(defn run-command
  [s]
  (let [fargs (str/split s #" ")
        keyw (keyword (first fargs))
        args (rest fargs)
        n (when (and (first args) (re-matches #"\d+" (first args))) (Integer/parseInt (first args)))]
    (try
      (when-let [command (-> @editor/state ::editor/commands keyw)]
        (let [m (or (meta command) {})]
          (cond (and (m :buffer) n) (apply-to-buffer #(command % n))
                (m :buffer) (apply-to-buffer #(command %))
                true (apply command args))))
      (catch Exception e (editor/message (str "Exception: " (.getMessage e)))))))

(defn abort-minibuffer
  []
  (editor/switch-to-buffer (@state ::bufferid)))
 
(defn execute
  []
  (let [content (buffer/text (editor/current-buffer))]
        ;[command param] (str/split content #" " 2)
        
    (apply-to-buffer buffer/clear)
    (editor/paint-buffer)
    (abort-minibuffer)
    ;(editor/previous-buffer)
    (cond (= (count content) 0) (do)
          (= (first content) \:) (run-command (subs content 1)) 
          (= (first content) \/) (apply-to-buffer #(buffer/search % (subs content 1)))
          (re-matches #"^M-x .*" content) (run-command (subs content 4))
          true (do)))) ; Maybe eval?

(defn run
  [prefix]
  (swap! state assoc ::bufferid ((editor/current-buffer) ::editor/id))
  (switch-to-buffer "*minibuffer*")
  (editor/apply-to-buffer #(-> % buffer/clear
                                 (buffer/insert-string prefix)
                                 buffer/set-insert-mode
                                 buffer/end-of-buffer
                                 buffer/right)))


(def mode
  {:commands {":tt" (fn [t] (editor/message (buffer/sexp-at-point (editor/current-buffer))))}
   :insert {"esc" abort-minibuffer
            "backspace" (fn [] (apply-to-buffer #(if (> (-> % ::buffer/cursor ::buffer/col) 1) (-> % buffer/left buffer/delete-char) %)))
            "\t" commands-completion
            "\n" execute}
   :normal {"esc" (fn [] (apply-to-buffer #(assoc % ::buffer/mode :insert)))
            "l" #(apply-to-buffer buffer/right)}
   :init run})
