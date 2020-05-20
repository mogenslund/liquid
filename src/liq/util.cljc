(ns liq.util
  (:require [clojure.string :as str]
            #?(:clj [clojure.java.io :as io]
              ; :cljs [lumo.io :as io :refer [slurp spit]]
              )
            #?(:cljs [cljs.js :refer [eval eval-str empty-state]])))

(def counter (atom 0))
(defn counter-next
  []
  (swap! counter inc))

(defn int-value
  [s]
  #?(:clj (if (int? s) s (Integer/parseInt s))
     :cljs (if (int? s) s (js/parseInt s))))

(defn now
  "Current time in milliseconds"
  []
  #?(:clj (System/currentTimeMillis)
     :cljs (.getTime (js/Date.))))

(defn windows?
  []
  (boolean (re-find #"(?i)windows" (System/getProperty "os.name"))))

(defn sleep
  ""
  [ms]
  #?(:clj (Thread/sleep ms)
     :cljs (do)))

(defn folder?
  [filepath]
  (.isDirectory (io/file filepath)))

(defn file?
  [filepath]
  (.isFile (io/file filepath)))

(defn exists?
  [filepath]
  (.exists (io/file filepath)))

(defn get-folder
  [filepath]
  (if (folder? filepath)
    filepath
    (str (.getParent (io/file filepath)))))

(defn resolve-home
  [path]
  (.getCanonicalPath (io/file (str/replace path #"^~" (str/replace (System/getProperty "user.home") "\\" "\\\\")))))

(defn resolve-path
  [part alternative-parent]
  (cond (.isAbsolute (io/file part)) (.getCanonicalPath (io/file part))
        (re-find #"^~" part) (str (.getCanonicalPath (io/file (str/replace part #"^~" (System/getProperty "user.home")))))
        true (str (.getCanonicalPath (io/file alternative-parent part)))))

(defn file
  ([folder filename]
    (str (io/file folder filename)))
  ([filepath]
    (str (io/file filepath))))

(defn filename
  [filepath]
  (str (.getName (io/file filepath))))

(defn parent
  [filepath]
  ;; if root return nil
  )

(defn absolute
  [filepath]
  (.getAbsolutePath (io/file filepath)))

(defn canonical
  [filepath]
  (.getCanonicalPath (io/file filepath)))

(defn tmp-file
  [filename]
  (str (io/file (System/getProperty "java.io.tmpdir") filename)))

(defn get-roots
  []
  (map str (java.io.File/listRoots)))

(defn get-children
  [filepath]
  (map str (.listFiles (io/file filepath))))

(defn get-folders
  [filepath]
  (map str (filter #(.isDirectory %) (.listFiles (io/file filepath)))))

(defn get-files
  [filepath]
  (filter file? (map str (.listFiles (io/file filepath)))))

(defn read-file
  [path]
  #?(:clj (when (.exists (io/file path))
            (slurp path))
     :cljs (do) ;(slurp path)
     ))

(defn write-file
  [path content]
  (io/make-parents path)
  (spit path content))

(defn eval-safe
  [text]
  #?(:clj (try
            (load-string text)
            (catch Exception e (str e)))
     :cljs (do (set! cljs.js/*eval-fn* cljs.js/js-eval) (eval-str (empty-state) text str))))

(comment
  (get-folder "/tmp/tmp.clj")
  (ls-files "/tmp")
  (ls-files "~")
  (ls-folders "/tmp")
  (ls-folders "~")
  (get-parent-folder "~"))

(def localclipboard (atom ""))
(def line? (atom false))

(defn clipboard-content
  []
  #?(:clj (if (java.awt.GraphicsEnvironment/isHeadless)
            @localclipboard
            (let [clipboard (.getSystemClipboard (java.awt.Toolkit/getDefaultToolkit))]
              (try
                (.getTransferData (.getContents clipboard nil) (java.awt.datatransfer.DataFlavor/stringFlavor))
                (catch Exception e ""))))
     :cljs @localclipboard))

(defn clipboard-line?
  []
  @line?)

(defn set-clipboard-content
  ([text line]
   #?(:clj (do (reset! localclipboard text)
               (reset! line? line)
               (when (not (java.awt.GraphicsEnvironment/isHeadless))
                 (let [clipboard (.getSystemClipboard (java.awt.Toolkit/getDefaultToolkit))]
                   (.setContents clipboard (java.awt.datatransfer.StringSelection. text) nil))))
      :cljs (do (reset! localclipboard text)
                (reset! line? line))))
  ([text] (set-clipboard-content text false)))

(defn pretty-exception
  [e]
  (let [message (.getMessage e)
        cause (.getCause e)
        stacklines (map str (.getStackTrace e))
        filtered (filter #(re-find (re-pattern #"liq") %) stacklines)
        shortened (map #(re-find (re-pattern "\\w*\\.clj:\\d+") %) filtered)]
    (str
     message "\n"
     cause "\n"
     (str/join "\n" shortened) "\n"
     (str/join "\n" filtered) "\n"
     (str/join "\n" stacklines))))

