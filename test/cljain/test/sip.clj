(ns ^{:doc "place doc string here"
      :author "ruiyun"}
  cljain.test.sip
  (:use clojure.test
        clojure.xml
        cljain.sip
        cljain.address)
  (:import [javax.sip SipProvider SipStack]
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
                (is (= (.getMethod request) Request/MESSAGE)))
              (getNewCallId [this]
                "1234567890")
              (getSipStack [this]
                (reify SipStack
                  (getStackName [this]
                    "test"))))
            cljain.sip/account-map (atom {"test" {:user "bob" :domain "test.com" :display-name "Bob"}})]
    (let [content "<Books Catlog='IT'><Book><Name>Clojure in Action</Name></Book></Books>"])
    (send-request! :MESSAGE :pack (xml content) :to (sip-uri "192.168.1.2"))))
