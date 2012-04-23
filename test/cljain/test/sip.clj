(ns ^{:doc "place doc string here"
      :author "ruiyun"}
  cljain.test.sip
  (:use clojure.test
        clojure.xml
        cljain.sip
        cljain.address)
  (:import [javax.sip SipProvider]
           [javax.sip.message Request]))

(defn xml [content]
  {:content-type :TEXT
   :content-sub-type :XML
   :content-length (count content)
   :content content})

(deftest send-message-no-transaction
  (binding [*sip-provider*
            (reify SipProvider
              (sendRequest [this request]
                (is (= (.getMethod request) Request/MESSAGE))))]
    (let [content "<Books Catlog='IT'><Book><Name>Clojure in Action</Name></Book></Books>"])
    (send-request :MESSAGE :pack (xml content) :to (sip-uri "192.168.1.2") :at (sip-uri ("192.168.1.3")))))
