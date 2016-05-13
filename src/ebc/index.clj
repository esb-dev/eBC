; eBooks Collection ebc - index
; Generating and searching the Lucene index from the ebooks in a 
; collection

; Copyright (c) 2014 - 2016 Burkhardt Renz, THM. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php).
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.

(ns ebc.index
  (:require [ebc.consts :as c]
            [ebc.util :refer :all]
            [clojure.string :as str]
            [clucy.core :as luc]
            [clojure.java.io :as io]
            [pantomime.extract :as extract])
  (:import
    (java.io File)
    (org.apache.lucene.analysis.standard StandardAnalyzer)
    (org.apache.lucene.analysis.util CharArraySet)
    (org.apache.lucene.util Version)))

#_(set! *warn-on-reflection* true)

;; ## Lucene adaption: the analyzer

(def 
  ^{:doc "Stopwords for the Lucene Analyzer"}
  stopwords 
  (CharArraySet. Version/LUCENE_44
    [ ; english words
    "a", "an", "and", "are", "as", "at", "be", "but", "by",
		"for", "if", "in", "into", "is", "it",
		"no", "not", "of", "on", "or", "such",
		"that", "the", "their", "then", "there", "these",
		"they", "this", "to", "was", "will", "with",
		;german words
    "einer", "eine", "eines", "einem", "einen",
		"der", "die", "das", "dass", "daß",
		"du", "er", "sie", "es",
		"was", "wer", "wie", "wir",
		"und", "oder", "ohne", "mit",
		"am", "im", "in", "aus", "auf",
		"ist", "sein", "war", "wird",
		"ihr", "ihre", "ihres",
		"als", "für", "von", "mit",
		"dich", "dir", "mich", "mir",
		"mein", "sein", "kein",
		"durch", "wegen", "wird"]
    true))

(def 
  ^{:doc "The Analyzer for ebooks in our collections"}
  ebc-analyzer (StandardAnalyzer. Version/LUCENE_44 ^CharArraySet stopwords))

;; ## Reporting the progress of generating oder updating the index

(def log-agent (agent {:msg ""}))

(defn init-log-agent 
  "Initializes the log-agent and reports the given msg."
  [msg report-fn] 
  (add-watch log-agent ::log-record
             (fn [_ _ _ new-val]
               (report-fn (:msg new-val))))
  (send log-agent assoc :msg msg))

(defn log-msg
  "Send the msg to the log-agent."
  [msg]
  (send log-agent assoc :msg msg))

(defn log-book
  "Sends the path of the current book to the log-agent,
   returns book -- to be used as a map fn."
  [book]
  (send log-agent assoc :msg (:path book))
  book)

;; ## Functions that prepare the map for the Lucene index

(defn extract-default
  "Extracts text only from filename (fallback function)."
  [^File file]
  (let [path (.getPath file)
        relpath (peek (re-matches (:path c/ebc-pats) path))]
  (apply str (interpose " " (str/split relpath #"[_/.]")))))

(defn extract-content
  "Extracts content from ebook 
   corresponding to the extension of the ebook."
  [file ext]
  (let [content (extract-default file)]
    (case ext
      ("txt" "htm" "html" "pdf" "epub")
      (try (str content " " (:text (extract/parse file)))
           (catch Exception e content))
      content)))
  
;; Structure of maps (ldoc) for the Lucene index
(def 
  ^{:doc "Structure of maps for the Lucene index"}
  ldoc-schema
  {:_id [:path]  ; unique id for documents is :path
   :path    {:type "string" :analyzed false}
   :date    {:type "string" :analyzed false}
   :type    {:type "string" :analyzed false}
   :ext     {:type "string" :analyzed false}
   :author  {:type "string"}
   :title   {:type "string"}
   :content {:type "string" :stored false}
   :size    {:type "string" :analyzed false}})

(defn book->ldoc
  "Makes a ldoc for Lucene from a book."
  [book]
  {:path    (:path book)
   :date    (:date book)
   :type    (:type book)
   :ext     (:ext  book)
   :size    (:size book)
   :author  (sort-str (:sort-authors book))
   :title   (sort-str (:sort-title book))
   :content (extract-content (:file book) (:ext book))})
    
(defn ldocs
  "Makes a seq of ldocs from a seq of books."
  [books]
    (map book->ldoc (map log-book books)))

;; ## Indexing, we use the clucy wrapper for Lucene

(defn open-index
  "Opens a Lucene index on disk."
  [name]
  (luc/disk-index name ldoc-schema))

(defn fill-index
  "Adds luc maps to the Lucene index."
  [index basedir report-fn]
  (binding [luc/*analyzer* ebc-analyzer
            luc/*content*  false]
   (apply luc/add index (ldocs (books basedir)))))

(defn make-index
  "Creates a new Lucene index on disk,
   deletes previous index, if found."
  [basedir report-fn]
  (report-fn (:ver c/ebc-rev))
  (init-log-agent (str "Indexing " basedir "...") report-fn)
  (try
	  (let [idx (str basedir "/" (:index c/ebc-file-names))]
	    (if (.exists (io/as-file idx))
	      (delete-rec idx))
	    (let [luc-idx (open-index idx)]
	      (fill-index luc-idx basedir report-fn)))
	  (log-msg "Done.")
  (catch Exception e 
    (log-msg "Could not generate the Lucene Index.")
    (log-msg (.getMessage e)))))

;; Searching the Lucene index

(defn search-index 
  "Searches the Lucene index in the basedir for n top docs
   using the given search-crit."
  [basedir search-crit]
  (let [idx-path (str basedir "/" (:index c/ebc-file-names))
        index (luc/disk-index idx-path)]
    (binding [luc/*analyzer* ebc-analyzer]
      (luc/search index search-crit 100 
         :default-field (:def-fld c/luc-config) :default-operator (:def-op c/luc-config)))))

(defn sresults
  "Structures the result of the search convenient for the generation of the html page."
  [basedir search-crit]
  {:title search-crit :links (into [] (search-index basedir search-crit))})
