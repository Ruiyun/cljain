(ns ^{:doc "place doc string here"
      :author "ruiyun"}
  cljain.sip
  (:use cljain.util
        [clojure.string :only [upper-case]])
  (:require [cljain.core :as core]
            [cljain.header :as header]
            [cljain.address :as addr]
            [cljain.message :as msg]
            [cljain.dialog :as dlg]))

(defn process-request
  "place doc string here"
  {:added "0.2.0"}
  [process-out-dialog-request evt]
  (when (nil? (.getServerTransaction evt))
    (let [dialog (.getDialog evt)
          request (.getRequest evt)
          transaction (.getNewServerTransaction evt)]
      (if (nil? dialog)
        (process-out-dialog-request request transaction)
        (let [process-in-dialog-request (:on-request (.getApplicationData dialog))]
          (and process-in-dialog-request (process-in-dialog-request request dialog transaction)))))))

(def ^{:doc "place doc string here"
       :added "0.2.0"
       :dynamic true}
  account-map (atom {}))

(defn account
  "Get current bound account information."
  {:added "0.2.0"}
  []
  (get @account-map (core/stack-name)))

(defn start!
  "Start to run with the current bound provider."
  {:added "0.2.0"}
  [& account-info]
  {:pre [(even? (count account-info))
         (check-optional account-info :user string?)
         (check-optional account-info :domain string?)
         (check-optional account-info :display-name string?)]}
  (let [{:keys [user domain display-name]} (apply hash-map account-info)]
    (core/start!)
    (swap! account-map assoc (core/stack-name) {:user user, :domain domain, :display-name display-name})))

(defn stop-and-release!
  "Stop to run with the current bound provider."
  {:added "0.2.0"}
  []
  (swap! account-map dissoc (core/stack-name))
  (core/stop-and-release!))

(defn running?
  "Check whether the current bound provider is running."
  {:added "0.2.0"}
  []
  (contains? account-map (core/stack-name)))

(defn legal-content?
  "Check the content is a string or a map with :type, :sub-type, :length and :content keys."
  {:added "0.2.0"}
  [content]
  (or (string? content)
    (and (map? content)
      (= (sort [:type :sub-type :content]) (sort (keys content))))))

(defn send-request!
  "Fluent style sip message send function.

  The simplest example just send a trivial MESSAGE:
  (send-request! \"MESSAGE\" :to (address (uri \"192.168.1.128\"))

  More complicate example:
  (let [bob (address (uri \"dream.com\" :user \"bob\") \"Bob\")
       [alice (address (uri \"dream.com\" :user \"alice\") \"Alice\")]
    (send-request! \"MESSAGE\" :pack \"Welcome\" :to bob :from alice
      :use-endpoint \"UDP\" :in :new-transaction :on-response #(prn %))

  If the pack content is not just a trivial string, provide a well named funciont
  to return a content map is recommended.
  {:type \"application\"
   :sub-type \"pidf-diff+xml\"
   :content content-object}"
  {:added "0.2.0"}
  [message & options]
  {:pre [(even? (count options))
         (check-optional options :pack legal-content?)
         (check-required options :to addr/address?)
         (check-optional options :from addr/address?)
         (check-optional options :use-endpoint :by upper-case in? ["UDP" "TCP"])
         (check-optional options :in #(or (in? % [:new-dialog]) (dlg/dialog? %)))
         (check-optional options :more-headers vector?)
         (check-optional options :on-response fn?)
         (check-optional options :on-timeout fn?)
         (check-optional options :on-transaction-terminated fn?)]}
  (let [{:keys [pack to from use-endpoint in more-headers
                on-response on-timeout on-transaction-terminated]} (apply hash-map options)
        {:keys [user domain display-name]} (account)
        {:keys [ip port transport]} (if (nil? use-endpoint)
                                      (core/listening-point)
                                      (core/listening-point use-endpoint))
        domain          (or domain ip)
        method          (upper-case (name message))
        request-uri     (addr/uri-from-address to)
        from-header     (if (nil? from)
                          (header/from (addr/address (addr/sip-uri domain :user user) display-name) (header/gen-tag))
                          (header/from from (header/gen-tag)))
        to-header       (if (= method "REGISTER") ; for REGISTER, To header's uri should equal the From header's.
                          (header/to (header/get-address from-header) nil) ; out of dialog, do not need tag
                          (header/to to nil)) ; TODO i don't know if need to pick the tag from exist dialog, try later.
        contact-header  (header/contact (addr/address (addr/sip-uri ip :port port :transport transport :user user)))
        call-id-header  (core/gen-call-id-header)
        via-header      (header/via ip port transport nil) ; via branch will be auto generated before message sent.
        request         (msg/request method request-uri from-header call-id-header to-header via-header contact-header
                          more-headers)]
    (cond
      (string? pack) (msg/set-content request (header/content-type "text" "plain") pack)
      (map? pack) (msg/set-content request (header/content-type (name (:type pack)) (name (:sub-type pack)))
                    (:content pack)))
    (core/send-request! request)
    request))

;; 重构思路
;; (binding [*sip-provider*]
;;   (send ccdp (xml...) :to (uri "192.168.1.2") :by "MESSAGE" :at (uri ("192.168.1.3") :on-response rsp-handler
;       :on-timeout timeout-handler))
;; ccdp is a function, return {:content-type "TEXT" :content-sub-type "UDP" :content-length 123 :content byte[]}
