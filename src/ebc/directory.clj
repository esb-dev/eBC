; eBooks Collection ebc - HTML directory of an eBook Collection
; The functions in this namespace check the naming conventions 
; and provide the data for the generation of the html pages.

; Copyright (c) 2014 - 2016 Burkhardt Renz, THM. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php).
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.

(ns ebc.directory
  (:require [ebc.consts :as c]
            [ebc.util :refer :all]
            [ebc.html :as html]
            [clojure.java.io :as io]
            [clojure.string :as str]))


#_(set! *warn-on-reflection* true)

;; Checking file names
(defn do-check
  "Checks the filenames in the ebooks collection in basedir and
   reports result on stdout."
  [basedir report-fn]
  (report-fn (:ver c/ebc-rev))
  (report-fn (str "Analyzing " basedir " ..."))
  (loop [books-seq (books basedir), i 1, result ()]
    (if (empty? books-seq)
      (do
        (report-fn (str "Checked " i " eBooks."))
        (if (empty? result)
          (report-fn "No errors found.")
          (do
            (report-fn "Please check the following files:")
            (doseq [fname result]
              (report-fn (str "- " fname))))))
      (let [name (:name (first books-seq))]
        (if (zero? (rem i 100))
          (report-fn (str "Analyzing ... "  i " ...")))
        (recur (rest books-seq) (inc i) (if (ebc-name? name) result (conj result name)))))))

;; The following function is used to define the groups for
;; authors, titles, and dates

(defn make-filter
  "Returns filter function for the value at the given keyword >= left and < right."
  [keyw left right]
  (fn [x] (let [p (keyw x)]
            (and (>= (compare p left) 0) (neg? (compare p right))))))

;; Definition of the groups for authors, titles and dates

(def 
  ^{:doc "Groups for the directory of books ordered by authors"}
  author-grps
  [{:caption "Authors, A-B", :flt (make-filter :sort-author (sort-key "0") (sort-key "C")) :html "ebc-a1.html"}
   {:caption "Authors, C-E", :flt (make-filter :sort-author (sort-key "C") (sort-key "F")) :html "ebc-a2.html"}
   {:caption "Authors, F-G", :flt (make-filter :sort-author (sort-key "F") (sort-key "H")) :html "ebc-a3.html"}
   {:caption "Authors, H-K", :flt (make-filter :sort-author (sort-key "H") (sort-key "L")) :html "ebc-a4.html"}
   {:caption "Authors, L-M", :flt (make-filter :sort-author (sort-key "L") (sort-key "N")) :html "ebc-a5.html"}
   {:caption "Authors, N-R", :flt (make-filter :sort-author (sort-key "N") (sort-key "S")) :html "ebc-a6.html"}
   {:caption "Authors, S-T", :flt (make-filter :sort-author (sort-key "S") (sort-key "U")) :html "ebc-a7.html"}
   {:caption "Authors, U-Z", :flt (make-filter :sort-author (sort-key "U") (sort-key "ZZ")) :html "ebc-a8.html"}])

(def 
  ^{:doc "Groups for the directory of books ordered by titles"}
  title-grps
  [{:caption "Titles, A-E", :flt (make-filter :sort-title (sort-key "0") (sort-key "F")) :html "ebc-t1.html"}
   {:caption "Titles, F-L", :flt (make-filter :sort-title (sort-key "F") (sort-key "M")) :html "ebc-t2.html"}
   {:caption "Titles, M-R", :flt (make-filter :sort-title (sort-key "M") (sort-key "S")) :html "ebc-t3.html"}
   {:caption "Titles, S-Z", :flt (make-filter :sort-title (sort-key "S") (sort-key "ZZ")) :html "ebc-t4.html"}])

(def 
  ^{:doc "Groups for the directory of books ordered by dates"}
  date-grps
  [{:caption "Date, 1 month back",  :flt (make-filter :date (back 1) (back -1)) :html "ebc-d1.html"}
   {:caption "Date, 3 months back", :flt (make-filter :date (back 3) (back 1))  :html "ebc-d2.html"}
   {:caption "Date, 6 months back", :flt (make-filter :date (back 6) (back 3))  :html "ebc-d3.html"}
   {:caption "Date, 1 year back",   :flt (make-filter :date (back 12) (back 6)) :html "ebc-d4.html"}])


;; Collecting the categories and subcategories for the directories according to subjects

(defn by-cat-subcat
  "Compares subjects lexicographically :cat, :subcat."
  [x y]
  (let [c (compare (:sort-cat x) (:sort-cat y))]
    (if (not= c 0)            
      c
       (compare (:sort-subcat x) (:sort-subcat y)))))

(defn subjects
  "Gives a sorted seq of unique subjects from the basedir,
   together with the name of the corresponding html file."
  [basedir]
  (sort by-cat-subcat
     (map-indexed #(assoc %2 :cat (sort-str (:sort-cat %2))
                             :subcat (sort-str (:sort-subcat %2))
                             :html (format "ebc-c%03d.html" %1))
        (scats basedir))))

(defn cat-grps
  "The groups for the directories with respect to cats and subcats,
   i.e. {:caption :html}."
  ([subjects] (cat-grps subjects ""))
  ([subjects cat]
    (let [flt  (fn [{:keys [cat subcat]} test-cat] (or (= cat test-cat) (empty? subcat)))
          imap (fn [{:keys [cat subcat html]}] 
                 (hash-map :caption (if (empty? subcat) cat (str "--, " subcat)), :html html))]
		  (map imap(filter #(flt % cat) subjects)))))

(defn navdata
  "Data for the navigation in the categories, or
   for the navigation with subcats of the given category."
  ([subjects] (navdata subjects ""))
  ([subjects cat]
    (let [clinks (vec (cat-grps subjects cat))
          alinks (vec (map #(select-keys % [:caption :html]) author-grps))
          tlinks (vec (map #(select-keys % [:caption :html]) title-grps))
          dlinks (vec (map #(select-keys % [:caption :html]) date-grps))]
      [{:title "Categories" :links clinks}
       {:title "Authors"    :links alinks}
       {:title "Titles"     :links tlinks}
       {:title "Dates"      :links dlinks}])))


;; authors gives all books in the collection 
;; filtered by filter-fn and sorted with respect to author and title
;; path author title type ext size date

(defn book->author
  "Takes a book and returns a seq of maps one for each author."
  [{:keys [path sort-authors sort-title type ext size date]}] 
  (for [author (str/split (sort-str sort-authors) #"\+")] 
    (hash-map :path path, :author author, :sort-author (sort-key author),
              :title (sort-str sort-title), :sort-title sort-title,
              :type type :ext ext :size size :date date)))
  
(defn- by-author-title
  "Lexicographical comparison with respect to author and title."
  [x y]
  (let [a (compare (:sort-author x) (:sort-author y))] 
    (if (not= a 0)
      a
      (compare (:sort-title x) (:sort-title y)))))
  
(defn authors
  "Sorted sequence books ordered by author, title."
  [books filter-fn]
	(let [unsorted-authors (filter filter-fn (mapcat book->author books))]
    (sort by-author-title unsorted-authors)))

(defn adata
  [books filter-fn]
  (let [auts (authors books filter-fn)
        grp (group-by #(sort-key (str (first (:author %)))) auts)]
    (map #(hash-map :title (sort-str (key %)) :links (val %)) grp)))

; titles -- a projection of books sorted with respect to titles
; path title authors type ext size date

(defn titles
  [books filter-fn]
  (let [unsorted-titles (map #(assoc % :title (sort-str (:sort-title %)))
                             (filter filter-fn books))]
    (sort-by :sort-title unsorted-titles)))

(defn tdata
  [books filter-fn]
  (let [tits (titles books filter-fn)
        grp (group-by #(sort-key (str (first (:title %)))) tits)]
    (map #(hash-map :title (sort-str (key %)) :links (val %)) grp)))

; dates -- a projection of books sorted with respect to dates
; path authors title type ext size date

(defn dates
  [books filter-fn]
  (let [unsorted-dates (filter filter-fn books)]
    (sort #(* -1 (compare (:date %1) (:date %2))) unsorted-dates)))

(defn ddata
  [books filter-fn]
  (let [dats (dates books filter-fn)
        grp (group-by :date dats)
        grp2 (sort #(* -1 (compare (key %1) (key %2))) grp)]
    (map #(hash-map :title (key %) :links (val %)) grp2)))


;; data for cats sorted with respect to topics, authors, and title
; path authors title type ext size date

(defn- by-topic-authors
  "Lexicographical comparison with respect to topic, authors and title."
  [x y]
  (let [t (compare (:sort-topic x) (:sort-topic y))] 
    (if (not= t 0)
      t
      (let [a (compare (:sort-authors x) (:sort-authors y))]
        (if (not= a 0)
          a
          (compare (:sort-title x) (:sort-title y)))
        ))))


(defn categories
  [books cat subcat]
   (let [flt (fn [book] (and (= cat (sort-str (:sort-cat book))) (= subcat (sort-str (:sort-subcat book)))))
         unsorted-cats (filter flt books)]
     (sort by-topic-authors unsorted-cats)))

(defn cdata
  [books cat subcat]
  (let [cats (categories books cat subcat)
        grp (group-by :sort-topic cats)]
    (map #(hash-map :title (key %) :links (val %)) grp)))


;; finally generating the directory pages

(defn- make-index
  [ndata basedir report-fn]
  (report-fn "generating home page...")
  (let [ipath (str basedir "/" (:home c/ebc-file-names))
	      ipage (html/indexp "eBC - Directory" ndata)]
    (html/spit-page ipath ipage)))

(defn- make-authors
  [ndata books basedir report-fn]
  (report-fn "generating author pages...")
  (doseq [{:keys [caption html flt]} author-grps]
    (report-fn caption)
    (let [path (str basedir "/" html)
          data (adata books flt)
          page (html/alinks caption ndata data)]
     (html/spit-page path page))))

(defn- make-titles
  [ndata books basedir report-fn]
  (report-fn "generating title pages...")
  (doseq [{:keys [caption html flt]} title-grps]
    (report-fn caption)
    (let [path (str basedir "/" html)
          data (tdata books flt)
          page (html/tlinks caption ndata data)]
      (html/spit-page path page))))

(defn- make-dates
  [ndata books basedir report-fn]
  (report-fn "generating date pages...")
  (doseq [{:keys [caption html flt]} date-grps]
    (report-fn caption)
    (let [path (str basedir "/" html )
          data (ddata books flt)
          page (html/dlinks caption ndata data)]
     (html/spit-page path page))))

(defn- make-cats
  [books subjects basedir report-fn]
  (doseq [{:keys [cat subcat html]} subjects]
    (report-fn (str "generating " cat " " subcat "..."))
    (let [ndata (navdata subjects cat)
          path (str basedir "/" html)
          data (cdata books cat subcat)
          title [subcat cat]
          page (html/clinks title ndata data)]
     (html/spit-page path page))))

(defn make-directory
  [basedir report-fn]
  (report-fn (:ver c/ebc-rev))
  (let [boks (books basedir)
        subjs (subjects basedir)]
      (let [ndata (navdata subjs)]
          (let [name (:css c/ebc-file-names)]
            (copy-resource name (str basedir "/" name)))
          (make-index   ndata basedir report-fn)
          (make-authors ndata boks basedir report-fn)
          (make-titles  ndata boks basedir report-fn)
          (make-dates   ndata boks basedir report-fn))
       (make-cats boks subjs basedir report-fn))
  (report-fn "done."))
    