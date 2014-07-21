; eBooks Collection ebc - util 

; Copyright (c) 2014 Burkhardt Renz, THM. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php).
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.

;; ## Utility Functions

;; The namespace ebc.util contains some helper functions for the
;; eBooks Collection ebCollection.

(ns ebc.util
  (:require [ebc.consts :as c]
            [clojure.string :as str]
            [clojure.java.io :as io])
  (:import (java.io File)
           (java.util Date)
           (java.text SimpleDateFormat Collator CollationKey)))

#_(set! *warn-on-reflection* true)

;; ## Utilities for Path and File Handling

(defn norm-path
  "Normalizes path separator to '/'."
  [^String path]
  (.replace path \\ \/))

(defn trim-path
  "Trims basedir from path.
   Return empty string id basedir = path.
   :pre basedir is prefix of path
   :pre basedir may not have a trailing path separator.
   :pre basedir and path use the same path separator."
  [^String path ^String basedir]
  (if (= path basedir)
    ""
    (let [len (inc (count basedir))]
      (subs path len))))

(defn delete-rec
  "Deletes files recursively and silently.
   Caveat: will not work with symbolic links!
   See: https://groups.google.com/forum/#!topic/clojure/LF3au6OZehM"
  [fname]
  (let [f (io/file fname)]
    (if (.isDirectory f)
      (doseq [child (.listFiles f)]
        (delete-rec child)))
    (io/delete-file f true)))

(defn filesize 
  "Transform filesize in bytes into a 'human readable' format.
   (filesize ...) works up to TB."
  [bytes]
  (if (< bytes 1000) 
    (str bytes " B")
    (let [units  ["B" "KB" "MB" "GB" "TB"]
          digitGroups (min (int (/ (Math/log10 bytes) (Math/log10 1000))) 4)
          result (/ bytes (Math/pow 1000 digitGroups))
          unit   (nth units digitGroups) ]
      (if (== result (int result))
         (format "%d %s" (int result) unit)
         (format "%.1f %s" result unit)))))

(defn filedate
  "Modification date of file as yyyy-MM-dd"
  [^File file]
  (.format (SimpleDateFormat. "yyyy-MM-dd") 
     (java.util.Date. (.lastModified file))))
  
(defn
  newer?
  "Is modification date of file > moddate (yyyy-MM-dd)?"
  [file moddate]
  (> (compare (filedate file) moddate) 0))

(defn 
  copy-resource
  "Copies resource from jar to destination in file system"
  [name dest]
  (io/copy (.getResourceAsStream (clojure.lang.RT/baseLoader) name) 
           (io/file dest)))

; ## Localization, we want sort according to locale

;; this is the Collator with the default locale and
;; sorting according to base letters
(def 
  ^{:dynamic true
    :doc "Collator with the default locale and primary strength"}
    *collator* 
    (doto (Collator/getInstance)
            (.setStrength Collator/PRIMARY)))

(defn sort-key
  "Makes a collation key using our collator"
  [^String s]
  (.getCollationKey ^Collator *collator* s))

(defn sort-str
  "String sorted according to sort-key"
  [^CollationKey sort-key]
  (.getSourceString sort-key))

;; ## Utilities for date

(defn now
  "Current date and time in format yyyy-MM-dd HH:mm"
  []
  (.format (java.text.SimpleDateFormat. "yyyy-MM-dd HH:mm") 
     (java.util.Date.)))

(defn back
  "Date n months before today in format yyyy-MM-dd"
  [n]
  (let [cal (java.util.Calendar/getInstance)]
    (.add cal java.util.Calendar/MONTH (- n))
    (.format (java.text.SimpleDateFormat. "yyyy-MM-dd") 
      (.getTime cal))))
  

;; ## Utilities for paths and books in an eBCollection

(defn ebc-pname? 
  "Checks whether the pathname pname is valid according to the
   ebc naming conventions.
   :pre pname is a relative path and is normalized, i.e. has '/' as path separator."
  [pname]
  (let [parts (str/split pname #"/")
        folders (subvec parts 0 (dec (count parts)))
        fname (peek parts)]
    (and (every? #(some? (re-matches (:folder c/ebc-pats) %)) folders)
         (some? (re-matches (:book c/ebc-pats) fname)))))
;; Note that the check is not complete, The prefixes of the folders n an ebCollection
;; have actually to be in the series ec_ - es_ - et_!!
;; We omit this check supposing that the folder structure of the ebCollection is well-formed.
  
(defn ebc-name?
  "Checks whether the filename has type, author, title, ext."
  [name]
  (let [parts (re-matches c/ebc-name-pat name)]
    (= (count parts) (count c/ebc-name-parts))))
; note that the check is actually not complete, but sufficient for our purposes

(defn- file->book
  "Takes file of an ebook and gives a map
   {:file
    :path relative to basedir
    :name
    :sort-cat 
    :sort-subcat 
    :sort-topic 
    :sort-authors 
    :sort-title 
    :type 
    :ext  
    :size 
    :date}"
  [^File file basedir]
  (let [maybe-match (fn [regex string]
                      (if-let [found (second (re-matches regex string))]
                        found ""))
        path (trim-path (norm-path (.getPath file)) basedir)
        name (.getName file)
        parts (re-matches c/ebc-name-pat name)
        authors (nth parts (:authors c/ebc-name-parts) "")
        title   (nth parts (:title   c/ebc-name-parts) "")]
    {:file     file
     :path     path
     :name     name
     :sort-cat      (sort-key (maybe-match (:cat c/ebc-pats) path))
     :sort-subcat   (sort-key (maybe-match (:subcat c/ebc-pats) path))
     :sort-topic    (sort-key (maybe-match (:topic c/ebc-pats) path)) 
     :sort-authors  (sort-key authors)
     :sort-title    (sort-key title)
     :type     (nth parts (:type c/ebc-name-parts) "")
     :ext      (nth parts (:ext c/ebc-name-parts) "")
     :size     (filesize (.length file))
     :date     (filedate file)
    }))

(defn books
  "Sequence of ebooks in basedir satisfying pred on file"
  ([basedir]
    (books basedir (constantly true)))
  ([basedir pred]
    (let [allfiles (filter pred (file-seq (io/file basedir)))
          ebcfiles (filter #(ebc-pname? (trim-path (norm-path (.getPath ^File %)) basedir)) allfiles)]
          (map #(file->book % basedir) ebcfiles))))

  
(defn- file->scat
  "Takes file of an cat or subcat folder and gives a map
   {:sort-cat
    :sort-subcat}."
  [^File file basedir]
  (let [maybe-match (fn [regex string]
                      (if-let [found (second (re-matches regex string))]
                        found ""))
        path (trim-path (norm-path (.getPath file)) basedir)]
    {:sort-cat      (sort-key (maybe-match (:cat c/ebc-pats) path))
     :sort-subcat   (sort-key (maybe-match (:subcat c/ebc-pats) path))
    }))

(defn scats
  "Sequence of maps with {:cat, :subcat} of categories and subcategories in the collection
   in basedir."
  [basedir]
  (let [dirs     (filter #(.isDirectory ^File %) (file-seq (io/file basedir)))
        names    (filter #(re-matches (:scat c/ebc-pats) (.getName ^File %)) dirs)]
    (map #(file->scat % basedir) names)))