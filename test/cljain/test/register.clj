(ns ^{:author "ruiyun"}
  cljain.test.register)

(org.apache.log4j.PropertyConfigurator/configure "log4j.properties")

(use 'cljain.dum)
(require '[cljain.sip.core :as sip]
  '[cljain.sip.address :as addr])

(global-set-account :user "bob" :domain "test" :display-name "Bob" :password "123456")
(sip/global-bind-sip-provider! (sip/sip-provider! "my-app" "localhost" 6060 "udp" :outbound-proxy "127.0.0.1:5060"))
(sip/set-listener! (dum-listener))
(sip/start!)

(register-to (addr/address "sip:test") 40
  :on-success #(prn "success")
  :on-failure #(prn "failure")
  :on-refreshed #(prn "refreshed")
  :on-refresh-failed #(prn "refresh failed"))
