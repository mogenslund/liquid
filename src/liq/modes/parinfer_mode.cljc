(ns liq.modes.parinfer-mode
  "To use this mode parinfer-rust is required:
  https://github.com/eraserhd/parinfer-rust
  The command parinfer-rust should be available
  as a command line program."
  (:require [clojure.string :as str]
            [clojure.data.json :as json]
            [liq.editor :as editor :refer [apply-to-buffer switch-to-buffer get-buffer]]
            [clojure.java.shell :as shell]
            [liq.buffer :as buffer]))

;; ~/proj/parinfer-rust/plugin/parinfer.vim
;; (editor/apply-to-buffer "/tmp/tmp.clj" #(update % ::buffer/major-modes conj :parinfer-mode))
;; (editor/apply-to-buffer "/tmp/tmp.clj" #(update % ::buffer/major-modes (fn [l] (remove (fn [elem] (= elem :parinfer-mode)) l))))

(comment
  (swap! editor/state update ::editor/new-buffer-hooks conj
    (fn [buf]
      (if (and (buf ::buffer/filename) (re-matches #".*.(cljc?|md)" (buf ::buffer/filename)))
        (update buf ::buffer/major-modes conj :parinfer-mode)
        buf))))

(defn call-parinfer-rust
  [req]
  (try
    (shell/sh "parinfer-rust" "--input-format" "json" "--output-format" "json"
              :in (json/write-str req))
    (catch Exception e {:exit 2 :err (.getMessage e)})))

(comment
  (call-parinfer-rust
    {:mode "smart"
     :text "(d []\n    c)"
     :options {:cursorX 2
               :cursorLine 2
               :forceBalance true
               :prevCursorX 1
               :prevCursorLine 2
               :prevText "(d []\n   c)"}}))

(defn local-sub-area
  "Sub buffer around cursor"
  [buf]
  (let [r1 (loop [r (dec (-> buf ::buffer/cursor ::buffer/row))]
             (let [c (buffer/get-char buf {::buffer/row r ::buffer/col 1})]
               (cond (<= r 1) 1
                     (and (not= c \space) (not (nil? c))) r
                     true (recur (dec r)))))
        r2 (loop [r (inc (-> buf ::buffer/cursor ::buffer/row))]
             (let [c (buffer/get-char buf {::buffer/row r ::buffer/col 1})]
               (cond (>= r (buffer/line-count buf)) (buffer/line-count buf)
                     (and (not= c \space) (not (nil? c))) (dec r)
                     true (recur (inc r)))))]
    [r1 r2]))

(defn par-action
  [buf fun]
  (let [[r1 r2] (local-sub-area buf)
        buf0 (buffer/sub-buffer buf r1 r2)
        buf1 (fun buf0)
        req {:mode "smart"
             :text (buffer/text buf1)
             :options {:cursorX (dec (-> buf1 ::buffer/cursor ::buffer/col))
                       :cursorLine (dec (-> buf1 ::buffer/cursor ::buffer/row))
                       :forceBalance true
                       :prevCursorX (dec (-> buf0 ::buffer/cursor ::buffer/col))
                       :prevCursorLine (dec (-> buf0 ::buffer/cursor ::buffer/row))
                       :prevText (buffer/text buf0)}}
        res (call-parinfer-rust req)]
    (if (and (= (res :exit) 0)) 
      (let [out (json/read-str (res :out) :key-fn keyword)]
        (if (not (out :error))
          (-> buf
              (buffer/delete-region [{::buffer/row r1 ::buffer/col 1} {::buffer/row r2 ::buffer/col (buffer/col-count buf r2)}])
              ;(assoc ::buffer/cursor {::buffer/row (inc r1) ::buffer/col 1})
              (buffer/insert-string (out :text))
              (assoc ::buffer/cursor {::buffer/row (+ (out :cursorLine) r1) ::buffer/col (inc (out :cursorX))})
              (buffer/set-insert-mode))
          (do (future (editor/message (str (out :error)))) (fun buf))))
      (do ;(future (editor/message (str (res :err) " - " (res :out))))
          (fun buf)))))


(defn run
  []
  (let [id (editor/get-buffer-id-by-name "*parinfer-test*")]
    (if id
      (switch-to-buffer id)
      (editor/new-buffer "" {:major-modes (list :parinfer-mode :clojure-mode :fundamental-mode) :name "*parinfer*"}))
    (editor/apply-to-buffer
      #(-> %
           buffer/clear
           (buffer/insert-string "(defn abc\n  [a b]\n  (+ a b))\n\n(+ 1 2 3)")
           buffer/set-normal-mode))))

(def mode
  {:insert {" " (fn [] (apply-to-buffer (fn [buf] (par-action buf #(buffer/insert-char % \space)))))
            "\n" (fn [] (apply-to-buffer (fn [buf] (par-action buf #(buffer/insert-char % \newline)))))
            "(" (fn [] (apply-to-buffer (fn [buf] (par-action buf #(buffer/insert-char % \()))))
            "[" (fn [] (apply-to-buffer (fn [buf] (par-action buf #(buffer/insert-char % \[)))))
            "{" (fn [] (apply-to-buffer (fn [buf] (par-action buf #(buffer/insert-char % \{)))))
            ;"\"" (fn [] (apply-to-buffer (fn [buf] (par-action buf #(buffer/insert-char % (first "\""))))))
            "backspace" (fn [] (apply-to-buffer (fn [buf] (par-action buf buffer/delete-backward))))}
           
   :normal {}
   :init run})