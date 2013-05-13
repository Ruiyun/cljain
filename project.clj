(defproject cljain "0.6.0-SNAPSHOT"
  :description "Enjoy JAIN-SIP in Clojure's way."
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/tools.logging "0.2.3"]
                 [ruiyun/tools.timer "1.0.0"]
                 [javax.sip/jain-sip-api "1.2.1.4"]
                 [javax.sip/jain-sip-ri "1.2.166"]]
  :lein-release {:deploy-via :clojars}
  :warn-on-reflection true
  :min-lein-version "2.0.0"
  :url "https://github.com/Ruiyun/cljain"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.html"}
  :profiles {:dev {:dependencies [[log4j/log4j "1.2.16"]]
                   :plugins [[codox "0.6.4"]]}})
