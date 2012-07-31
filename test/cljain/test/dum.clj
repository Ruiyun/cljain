(ns ^{:author "ruiyun"}
  cljain.test.dum)

(org.apache.log4j.PropertyConfigurator/configure "log4j.properties")

(use 'cljain.dum)
(require '[cljain.sip.core :as sip]
  '[cljain.sip.address :as addr])

(defmethod handle-request :MESSAGE [request & [transaction]]
  (println "Received: " (.getContent request))
  (send-response! 200 :in transaction :pack "I receive the message from myself."))

(global-set-account :user "bob" :domain "localhost" :display-name "Bob")
(sip/global-bind-sip-provider! (sip/sip-provider! "my-app" "localhost" 5060 "udp"))
(sip/set-listener! (dum-listener))
(sip/start!)

(send-request! :MESSAGE :to (addr/address "sip:bob@localhost") :pack "Hello, Bob."
  :on-success (fn [& {:keys [response]}] (println "Fine! response: " (.getContent response)))
  :on-failure (fn [& {:keys [response]}] (println "Oops!" (.getStatusCode response)))
  :on-timeout (fn [_] (println "Timeout, try it later.")))
