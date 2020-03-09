(ns liq.core
  (:require [clojure.string :as str]
            [liq.modes.fundamental-mode :as fundamental-mode]
            [liq.modes.minibuffer-mode :as minibuffer-mode]
            [liq.modes.buffer-chooser-mode :as buffer-chooser-mode]
            [liq.modes.help-mode :as help-mode]
            [liq.modes.dired-mode :as dired-mode]
            [liq.modes.typeahead-mode :as typeahead-mode]
            [liq.modes.clojure-mode :as clojure-mode]
            [liq.modes.spacemacs-mode :as spacemacs-mode]
            [liq.modes.parinfer-mode :as parinfer-mode]
            [liq.modes.info-dialog-mode :as info-dialog-mode]
            #?(:clj [liq.extras.cool-stuff :as cool-stuff])
            #?(:clj [liq.jframe-io :as jframe-io])
            #?(:cljs [liq.browser-io :as browser-io])
            [liq.extras.markdownfolds :as markdownfolds]
            [liq.extras.snake-mode :as snake-mode]
            [liq.buffer :as buffer]
            [liq.editor :as editor]
            [liq.tty-input :as input]
            [liq.util :as util]
            [liq.commands :as commands]
            [liq.tty-output :as output])
  #?(:clj (:gen-class)))

#?(:cljs (enable-console-print!))

(defn- read-arg
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

(defn- read-arg-int
  [args arg]
  (let [strres (read-arg args arg)]
    (when strres (Integer/parseInt strres))))


(defn load-dot-liq
  []
  (try
    (let [path (util/resolve-home "~/.liq")]
      (when (util/exists? path)
        (load-file path)))
   (catch Exception e (editor/message (str "Error loading .liq:\n" e)))))

(defn load-extras
  []
  #?(:clj (cool-stuff/load-cool-stuff))
  (markdownfolds/load-markdownfolds)
  (swap! editor/state assoc-in [:liq.editor/modes :snake-mode] liq.extras.snake-mode/mode)
  (swap! editor/state assoc-in [:liq.editor/commands :snake] liq.extras.snake-mode/run)
  (swap! editor/state assoc-in [:liq.editor/commands :p]
    (fn [] (editor/apply-to-buffer #(update % ::buffer/major-modes conj :parinfer-mode))))
  (swap! editor/state assoc-in [:liq.editor/commands :parinfer]
    (fn [] (editor/apply-to-buffer #(update % ::buffer/major-modes conj :parinfer-mode))))
  (swap! editor/state assoc-in [:liq.editor/commands :parinferoff]
    (fn [] (editor/apply-to-buffer #(update % ::buffer/major-modes (fn [l] (remove (fn [elem] (= elem :parinfer-mode)) l))))))
  (swap! editor/state assoc-in [:liq.editor/commands :poff]
    (fn [] (editor/apply-to-buffer #(update % ::buffer/major-modes (fn [l] (remove (fn [elem] (= elem :parinfer-mode)) l)))))))


;; clj -m liq.experiments.core
(defn -main
  [& args]
  (swap! editor/state update ::editor/commands merge commands/commands)
  (editor/add-mode :fundamental-mode fundamental-mode/mode)
  (editor/add-mode :minibuffer-mode minibuffer-mode/mode)
  (editor/add-mode :buffer-chooser-mode buffer-chooser-mode/mode)
  (editor/add-mode :help-mode help-mode/mode)
  (editor/add-mode :typeahead-mode typeahead-mode/mode)
  (editor/add-mode :dired-mode dired-mode/mode)
  (editor/add-mode :clojure-mode clojure-mode/mode)
  (editor/add-mode :parinfer-mode parinfer-mode/mode)
  (editor/add-mode :info-dialog-mode info-dialog-mode/mode)
  ;(editor/add-mode :spacemacs-mode spacemacs-mode/mode)
  (spacemacs-mode/load-spacemacs-mode)
  (cond (read-arg args "--jframe")
        (do
          (editor/set-output-handler jframe-io/output-handler)
          (jframe-io/init editor/handle-input)
          (editor/set-exit-handler jframe-io/exit-handler))
        (read-arg args "--browser")
        (do
          #?(:cljs
              (do
                (editor/set-output-handler browser-io/output-handler)
                (browser-io/init editor/handle-input)
                (editor/set-exit-handler (fn [] (do))))))
        true
        (do
          (editor/set-output-handler output/output-handler)
          (input/init)
          (editor/set-exit-handler input/exit-handler)
          (input/input-handler editor/handle-input)))
  (let [w (editor/get-window)
        rows (w ::buffer/rows)
        cols (w ::buffer/cols w)]
    ;(editor/paint-buffer)
    (editor/new-buffer "" {:name "*status-line*" :top rows :left 1 :rows 1 :cols cols
                           :major-modes (list :fundamental-mode) :mode :insert})
    (editor/new-buffer "" {:name "*minibuffer*" :top rows :left 1 :rows 1 :cols cols
                           :major-modes (list :minibuffer-mode) :mode :insert})
    (if (read-arg args "--simple")
      (do
        (editor/new-buffer "" {:name "*output*" :top 1 :left 1 :rows (- rows 1) :cols cols :mode :normal})
        (editor/new-buffer "" {:name "scratch" :top 1 :left 1 :rows (- rows 1) :cols cols}))
      (do
        (editor/set-setting :auto-switch-to-output false)
        (editor/new-buffer "Output" {:name "*output*" :top (- rows 5) :left 1 :rows 5 :cols cols :mode :normal})
        (editor/new-buffer "-----------------------------" {:name "*delimeter*" :top (- rows 6) :left 1 :rows 1 :cols cols})
        (editor/new-buffer "" {:name "scratch" :top 1 :left 1 :rows (- rows 7) :cols cols :major-mode :clojure-mode})))
    (editor/paint-buffer)
    (load-extras)
    #?(:clj (load-dot-liq))))

#?(:cljs (set! cljs.core/*main-cli-fn* -main))
#?(:cljs (defn init [] (-main "--browser")))

;#?(:cljs
;(do
;(defn ^:export init
;  []
;  (.log js/console "Hello, world") (-main "--browser"))

;(enable-console-print!)

;(def mycounter (atom 0))

;(defn keydown [e]
;  ;(when (h/in? [32 37 38 39 40] (.-keyCode e)) (.preventDefault e))
;  (swap! mycounter inc)
;  (.log js/console e)
;  (-> js/document
;      (.getElementById "app")
;      (.-innerHTML)
;      (set! (str "<h1>It works</h1>" (.-key e) " " (.-keyCode e) " " @mycounter))))
;(set! (.-onkeydown js/document) keydown)

;))