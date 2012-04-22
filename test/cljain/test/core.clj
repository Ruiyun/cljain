(ns cljain.test.core
  (:use cljain.core)
  (:use clojure.test)
  (:import [javax.sip SipProvider]))

(deftest send-message-no-transaction
  (binding [*sip-provider* (reify SipProvider
                            (sendRequest [this request]
                              (is ())))]))

