; eBooks Collection ebc - index
; Generating and searching the Lucene index from the ebooks in a 
; collection

; Copyright (c) 2014 Burkhardt Renz, THM. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php).
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.

(ns ebc.index
  (:require [ebc.consts :as c]
            [ebc.util :refer :all]
            [clojure.string :as str]
            [clucy.core :as luc]
            [clojure.java.io :as io])
  (:import
    (java.io File)
    (org.apache.lucene.analysis.standard StandardAnalyzer)
    (org.apache.lucene.analysis.util CharArraySet)
    (org.apache.lucene.util Version AttributeSource)
    (org.apache.tika.parser Parser ParseContext)
    (org.apache.tika.parser.epub EpubParser)
    (org.apache.tika.parser.html HtmlParser)
    (org.apache.tika.parser.txt TXTParser)
    (org.apache.tika.metadata Metadata)
    (org.apache.tika.sax BodyContentHandler)
    (org.xml.sax SAXException)
    (com.itextpdf.text.pdf PdfReader)
    (com.itextpdf.text.pdf.parser PdfTextExtractor)))

#_(set! *warn-on-reflection* true)

;; ## Lucene adaption: the analyzer

(def 
  ^{:doc "Stopwords for the Lucene Analyzer"}
  stopwords 
  (CharArraySet. Version/LUCENE_CURRENT
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
  ebc-analyzer (StandardAnalyzer. Version/LUCENE_CURRENT ^CharArraySet stopwords))

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

(defn tika
  "Extracts text using the parser with as little help from Apache tika."
  [^Parser parser ^File file]
  (let [md  (Metadata. )
        bch (BodyContentHandler. (* 1024 1024))]
    (try
      (with-open [is (io/input-stream file)]
        (.parse parser is bch md (ParseContext. )))
      (.toString bch)
    (catch SAXException se ; e.g. limit for BodyContentHandler reached - content so far can be used
      (.toString bch))
    (catch Exception e     ; use infos from filename
      ""))))
    
(defn extract-epub
  "Extracts text from epub file with the help of Apache tika."
  [^File file]
  (tika (EpubParser. ) file))
 
(defn extract-html
  "Extracts text from html file with the help of Apache tika."
  [^File file]
  (tika (HtmlParser. ) file))

(defn extract-txt
  "Extracts text from txt file with the help of Apache tika."
  [^File file]
  (tika (TXTParser. ) file))

(defn extract-pdf
  "Extracts text from pdf file with a little help from iText."
  [file]
  (try
    (let [reader   (PdfReader. (io/input-stream file))
          page-cnt (.getNumberOfPages reader)
          content  (apply str (for [pg (range page-cnt)]
                                (PdfTextExtractor/getTextFromPage reader (inc pg))))]
      (if reader (.close reader))
       content)
    (catch Throwable e 
      "")))

  
(defmulti extract-content
  "Extracts content from ebook 
   corresponding to the extension of the ebook."
  (fn [file ext]
    ext))

(defmethod extract-content "txt"
  [file ext]
  (str (extract-default file) (extract-txt file)))

(defmethod extract-content "htm"
  [file ext]
  (str (extract-default file) (extract-html file)))
  
(defmethod extract-content "html"
  [file ext]
  (str (extract-default file) (extract-html file)))

(defmethod extract-content "pdf"
  [file ext]
  (str (extract-default file) (extract-pdf file)))

(defmethod extract-content "epub"
  [file ext]
  (str (extract-default file) (extract-epub file)))

(defmethod extract-content :default
  [file ext]
  (extract-default file))

;; Structure of maps (ldoc) for the Lucene index
(def 
  ^{:doc "Structure of maps for the Lucene index"}
  ldoc-schema
  {:_id [:path]  ; unique id for documents is :path
   :path    {:type "string" :analyzed false}
   :date    {:type "string" :analyzed false}
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
