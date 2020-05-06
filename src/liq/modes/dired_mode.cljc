(ns liq.modes.dired-mode
  (:require [clojure.string :as str]
            [liq.editor :as editor :refer [apply-to-buffer switch-to-buffer get-buffer]]
            [liq.buffer :as buffer]
            [liq.util :as util]))

(defn load-content
  [buf folder]
  (-> buf
      buffer/clear
      (buffer/insert-string
        (str
          (util/resolve-path folder ".") "\n../\n./\n"
          (str/join "\n" (sort (map #(str (util/filename %) "/") (util/get-folders folder))))
          "\n"
          (str/join "\n" (sort (map #(str (util/filename %)) (util/get-files folder))))))
      buffer/beginning-of-buffer
      buffer/down))

(defn run
  ([path]
   (let [f (or path ((editor/current-buffer) ::buffer/filename) ".")
         folder (util/absolute (util/get-folder f))
         id (editor/get-buffer-id-by-name "*dired*")]
     (if id
       (switch-to-buffer id)
       (editor/new-buffer "" {:major-modes (list :dired-mode) :name "*dired*"}))
     (apply-to-buffer #(load-content % folder))
     (editor/highlight-buffer)))
  ([] (run nil)))


(defn choose
  []
  (let [buf (editor/current-buffer)
        parent (buffer/line buf 1)
        f (buffer/line buf)
        path (str parent "/" f)]
    (when (> (-> buf ::buffer/cursor ::buffer/row) 1)
      (if (util/folder? path)
        (do 
          (apply-to-buffer #(assoc % ::buffer/cursor {::buffer/row 1 ::buffer/col 1}))
          (editor/paint-buffer)
          (apply-to-buffer #(load-content % path))
          (editor/highlight-buffer))
        (editor/open-file path)))))

(defn new-file
  []
  (let [buf (editor/current-buffer)
        parent (buffer/line buf 1)
        f (if (= (-> buf ::buffer/cursor ::buffer/row) 1) "" (buffer/line buf))
        path (str parent "/" f)]
    (((editor/get-mode :minibuffer-mode) :init) (str ":e " path))))

(def mode
  {:insert {"esc" (fn [] (apply-to-buffer #(-> % (assoc ::buffer/mode :normal) buffer/left)))
            "\n" choose}
   :normal {"q" editor/previous-buffer
            "esc" editor/previous-buffer
            "\n" choose
            "h" :left 
            "j" :down
            "k" :up
            "l" :right
            "C-b" :left
            "C-n" :down
            "C-p" :up
            "C-f" :right
            "left" :left 
            "down" :down 
            "up" :up 
            "right" :right
            "n" #(apply-to-buffer buffer/search)
            "0" #(apply-to-buffer buffer/beginning-of-line)
            "$" #(apply-to-buffer buffer/end-of-line)
            "g" {"g" #(editor/apply-to-buffer buffer/beginning-of-buffer)}
            "G" #(apply-to-buffer buffer/end-of-buffer)
            "%" new-file
            "/" #(((editor/get-mode :minibuffer-mode) :init) "/")
            ":" #(((editor/get-mode :minibuffer-mode) :init) ":")}
    :visual {"esc" #(apply-to-buffer buffer/set-normal-mode)}
    :init run
    :syntax
     {:plain ; Context
       {:style :plain ; style
        :matchers {#"^/.*$" :root-file
                   #"[^/]+/$" :folder}}
      :root-file
       {:style :definition
        :matchers {#".|$" :plain}}

      :folder
       {:style :keyword
        :matchers {#".|$" :plain}}}})