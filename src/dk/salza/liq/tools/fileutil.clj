(ns dk.salza.liq.tools.fileutil
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.test :as test]))

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

(defn folder?
  [filepath]
  (.isDirectory (io/file filepath)))

(defn file?
  [filepath]
  (.isFile (io/file filepath)))

(defn exists?
  [filepath]
  (.exists (io/file filepath)))

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
  
(defn read-file-to-list
  [filepath]
  (when (file? filepath)
    (with-open [r (io/reader filepath)]
      (rest (apply concat (map #(conj (map str (seq %)) "\n") (doall (line-seq r))))))))

(defn write-file
  [filepath content]
  (when (not (.isDirectory (io/file filepath)))
    (with-open [writer (io/writer filepath)]
      (.write writer content))))
  