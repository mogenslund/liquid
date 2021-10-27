(ns liq.util
  (:require [clojure.string :as str]
            #?(:clj [clojure.java.io :as io])
            #?(:bb [clojure.java.io :as io])
            #?(:cljs [cljs.js :refer [eval eval-str empty-state]])
            #?(:cljs [fs])
            #?(:cljs [path])
            #?(:cljs [os])))

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
  #?(:clj (boolean (re-find #"(?i)windows" (System/getProperty "os.name")))
     :cljs (boolean (re-find #"(?i)win" (.platform os)))))

(defn sleep
  ""
  [ms]
  #?(:clj (Thread/sleep ms)
     :cljs (do)))

(defn folder?
  [filepath]
  #?(:clj (.isDirectory (io/file filepath))
     :cljs (.isDirectory (fs/lstatSync filepath))))

(defn file?
  [filepath]
  #?(:clj (.isFile (io/file filepath))
     :cljs (.isFile (fs/lstatSync filepath))))

(defn exists?
  [filepath]
  #?(:clj (.exists (io/file filepath))
     :cljs (fs/existsSync filepath)))

(defn absolute
  [filepath]
  #?(:clj (.getAbsolutePath (io/file filepath))
     :cljs (path/resolve filepath)))

(defn absolute?
  [filepath]
  #?(:clj (.isAbsolute (io/file filepath))
     :cljs (path/isAbsolute filepath)))

(defn canonical
  [filepath]
  #?(:clj (.getCanonicalPath (io/file filepath))
     :cljs (path/resolve filepath)))

(defn get-folder
  [filepath]
  (cond (re-matches #"https?:.*" filepath) (re-find #"https?:.*/" filepath)
        (folder? filepath) filepath
        true #?(:clj (str (.getParent (io/file filepath)))
                :cljs (path/basename (path/dirname filepath)))))

(defn resolve-home
  [path]
  (cond (re-matches #"https?:.*" path) path
        true #?(:clj (.getCanonicalPath (io/file (str/replace path #"^~" (str/replace (System/getProperty "user.home") "\\" "\\\\"))))
                :cljs (str/replace path #"^~" (str/replace (os/homedir) "\\" "\\\\")))))

(defn resolve-path
  [part alternative-parent]
  (cond (nil? part) "."
        (re-matches #"https?:.*" part) part
        (re-matches #"https?:.*" alternative-parent) (str alternative-parent part)
        (absolute? part) (canonical part)
        (re-find #"^~" part) (resolve-home part)
        true (str (canonical #?(:clj (io/file alternative-parent part)
                                :cljs (path/join alternative-parent part))))))

(defn file
  ([folder filename]
   #?(:clj (str (io/file folder filename))
      :cljs (str (path/join folder filename))))
  ([filepath]
   #?(:clj (str (io/file filepath))
      :cljs (str (path/join filepath)))))

(defn filename
  [filepath]
  (str #?(:clj (.getName (io/file filepath))
          :cljs (path/basename filepath))))

(defn parent
  [filepath])
  ;; if root return nil

(defn tmp-file
  [filename]
  #?(:clj (str (io/file (System/getProperty "java.io.tmpdir") filename))
     :cljs (str (path/join (os/tmpdir) filename))))

(defn get-roots
  []
  #?(:clj (map str (java.io.File/listRoots))))

(defn get-children
  [filepath]
  (map str #?(:clj (.listFiles (io/file filepath))
              :cljs (fs/readdirSync filepath))))

(defn get-folders
  [filepath]
  (map str (filter folder? (get-children filepath))))

(defn get-files
  [filepath]
  (filter file? (map str  (get-children filepath))))

(defn read-file
  [path]
  #?(:clj (cond (re-matches #"https?:.*" path) (slurp path)
                (.exists (io/file path)) (slurp path))
     :cljs (fs/readFileSync path))) ;(slurp path)

(defn write-file
  [path content]
  #?(:clj (do (io/make-parents path)
              (spit path content))
     :cljs (fs/writeFileSync path content)))

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
  #?(:bb @localclipboard
     :clj (if (java.awt.GraphicsEnvironment/isHeadless)
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
   #?(:bb (do (reset! localclipboard text)
              (reset! line? line))
      :clj (do (reset! localclipboard text)
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

(defn shorten-path
  "Display shorter version of path"
  [path width]
  (if (<= (count path) width)
    path
    (let [slashes (count (re-seq #"[/\\]" path))
          p1 (loop [n 0]
               (let [p (str/replace-first
                         path
                         (re-pattern (str "(?<=[^/\\\\][/\\\\])([^/\\\\]*[/\\\\]){" n "}[^/\\\\]*"))
                         "...")]
                 (if (or (<= (count p) width) (>= n (- slashes 3)))
                   p 
                   (recur (inc n)))))
          p2 (str/replace-first path #".*(?=[/\\])" "...")]
      (if (<= (count p1) width)
        p1
        p2))))

(defn get-ns-by-name
  [name]
  (->> (all-ns)
       (filter #(= (-> % ns-name str) name))
       (first)))

