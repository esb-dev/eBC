; Copyright (c) 2014 Burkhardt Renz, THM. All rights reserved.

(ns ebc.util-test
  (:require [clojure.test :refer :all]
            [ebc.util :refer :all]))

(deftest test-norm-path
  (is (= (norm-path "c:\\lucene") "c:/lucene"))
  (is (= (norm-path "c:\\lucene\\bin") "c:/lucene/bin"))
  (is (= (norm-path "c:\\lucene\\.bin") "c:/lucene/.bin"))
  (is (= (norm-path "file://c:\\lucene\\.bin") "file://c:/lucene/.bin"))
)      

(deftest test-trim-path
  (is (= (trim-path "c:/lucene" "c:") "lucene"))
  (is (= (trim-path "c:/lucene/bin" "c:") "lucene/bin"))
  (is (= (trim-path "c:/ebooks/myCollection" "c:/ebooks") "myCollection"))
  (is (= (trim-path "./myCollection" ".") "myCollection"))
)

(deftest test-filesize
  (is (= (filesize 200) "200 B"))
  (is (= (filesize 999) "999 B"))
  (is (= (filesize 1000) "1 KB"))
  (is (= (filesize 1001) "1,0 KB"))
  (is (= (filesize 2100) "2,1 KB"))
  (is (= (filesize 1000000) "1 MB"))
  (is (= (filesize 3586970) "3,6 MB"))
  (is (= (filesize 1000000000) "1 GB"))
  (is (= (filesize 1000000000000) "1 TB"))
  (is (= (filesize 1000000000000000) "1000 TB"))
)  

(deftest test-sort-key
  (is (= (sort-str (sort-key "Hallo")) "Hallo")))

(deftest test-ebc-pname?
  (is (ebc-pname? "ec_Category/eb_Book.pdf"))
  (is (ebc-pname? "ec_Category/es_SubCategory/eb_Book.pdf"))
  (is (ebc-pname? "ec_Category/et_Topic/ex_Book.pdf"))
  (is (ebc-pname? "ec_Category/es_SubCategory/et_Topic/eb_Book.pdf"))
  ; the following is true but actually not allowed
  (is (ebc-pname? "ec_Category/ec_Category/et_Topic/eb_Book.pdf"))
  (is (not (ebc-pname? "Category/ec_Category/et_Topic/eb_Book.pdf")))
  (is (not (ebc-pname? "ec_Category//et_Topic/Book.pdf")))
)         
