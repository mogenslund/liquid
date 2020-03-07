(ns liq.modes.minibuffer-mode
  (:require [clojure.string :as str]
            [liq.util :as util]
            #?(:clj [liq.tools.shell :as s])
            [liq.editor :as editor :refer [apply-to-buffer switch-to-buffer get-buffer]]
            [liq.buffer :as buffer]))

(defn run-command
  [s]
  (let [fargs (str/split s #" ")
        keyw (keyword (first fargs))
        args (rest fargs)
        n (if (and (first args) (re-matches #"\d+" (first args))) (Integer/parseInt (first args)) 1)]
    (when-let [command (-> @editor/state ::editor/commands keyw)]
      (let [m (or (meta command) {})]
        (cond (m :motion) (apply-to-buffer #(command % n))
              true (apply command args))))))

(defn execute
  []
  (let [content (buffer/get-text (editor/current-buffer))
        ;[command param] (str/split content #" " 2)
        ]
    (apply-to-buffer buffer/clear)
    (editor/paint-buffer)
    (editor/previous-buffer)
    (cond (= (count content) 0) (do)
          (= (first content) \:) (run-command (subs content 1)) 
          (= (first content) \/) (apply-to-buffer #(buffer/search % (subs content 1)))
          (re-matches #"^M-x .*" content) (run-command (subs content 4))
          true (do)))) ; Maybe eval?


(def mode
  {:commands {":tt" (fn [t] (editor/message (buffer/sexp-at-point (editor/current-buffer))))}
   :insert {"esc" editor/previous-buffer
            "backspace" (fn [] (apply-to-buffer #(if (> (-> % ::buffer/cursor ::buffer/col) 1) (-> % buffer/left buffer/delete-char) %)))
            "\n" execute}
   :normal {"esc" (fn [] (apply-to-buffer #(assoc % ::buffer/mode :insert)))
            "l" #(apply-to-buffer buffer/right)}})
