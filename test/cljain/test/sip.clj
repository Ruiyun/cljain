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

;;---------------------------------------------------------------------------------------------

(load-file "src/cljain/core.clj")
(load-file "src/cljain/sip.clj")

(org.apache.log4j.PropertyConfigurator/configure "log4j.properties")

(def provider (cljain.core/sip-provider! "test" "127.0.0.1" 5060 "udp"))

(binding [cljain.core/*sip-provider* provider]
  ;(cljain.core/set-listener! :request #(prn %))
  (cljain.sip/start! :user "reuiyun" :domain "notbook" :display-name "Ruiyun Wen"))

(binding [cljain.core/*sip-provider* provider]
  (let [bob (cljain.address/address (cljain.address/sip-uri "127.0.0.1" :port 5070 :user "bob") "Uncle Bob")]
    (cljain.sip/send-request! :MESSAGE :to bob :pack "Welcome" :on-response #(prn "Recieve response" %))))

(binding [cljain.core/*sip-provider* provider]
  (cljain.sip/stop-and-release!))
