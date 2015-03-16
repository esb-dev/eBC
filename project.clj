; eBooks Collection ebc - Project definition 

;   Copyright (c) 2014 Burkhardt Renz, THM. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php).
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.

(defproject ebc "4.2.1"
  :description "eBooks Collection Toolbox"
  :url "http://homepages.thm.de/~hg11260/ebc.html"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]  
                 [org.clojure/tools.cli "0.3.1"]
                 [seesaw "1.4.4"]
                 [ceterumnet-zclucy "0.9.4"]
                 [enlive "1.1.5"]
                 [com.itextpdf/itextpdf "5.5.0"]
                 [org.apache.tika/tika-parsers "1.5"]]
  :main ebc.main
  :profiles {:uberjar  {:aot :all}}
  :uberjar-name "ebc.jar")
