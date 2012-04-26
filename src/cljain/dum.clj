(ns ^{:doc "place doc string here"
      :author "ruiyun"}
  cljain.dum
  (:use     cljain.util
            [clojure.string :only [upper-case]])
  (:require [cljain.sip.core :as core]
            [cljain.sip.header :as header]
            [cljain.sip.address :as addr]
            [cljain.sip.message :as msg]
            [cljain.sip.dialog :as dlg]
            [cljain.sip.transaction :as trans]))

(def ^{:doc "place doc string here"
       :added "0.2.0"
       :dynamic true}
  account-map (atom {}))

(defn account
  "Get current bound account information."
  {:added "0.2.0"}
  []
  {:pre [(core/already-bound-provider?)]}
  (get @account-map (core/stack-name)))

(defn set-account-info!
  "Set the default user account information with the current bound provider."
  {:added "0.2.0"}
  [& account-info]
  {:pre [(core/already-bound-provider?)
         (even? (count account-info))
         (check-optional account-info :user string?)
         (check-optional account-info :domain string?)
         (check-optional account-info :display-name string?)]}
  (let [{:keys [user domain display-name]} (apply array-map account-info)]
    (swap! account-map assoc (core/stack-name) {:user user, :domain domain, :display-name display-name})))

(defn remove-account-info!
  "remove user account information with the current bound provider."
  {:added "0.2.0"}
  []
  {:pre [(core/already-bound-provider?)]}
  (swap! account-map dissoc (core/stack-name)))

; TODO Do not user application data in transaction or dialog for event dispatch.

(defn- request-processor
  "Return a funcion to process request."
  {:added "0.2.0"}
  [process-new-dialog process-out-of-dialog-request]
  (fn [{:keys [request server-transaction dialog]}]
    (if (nil? dialog)
      (let [server-transaction (or server-transaction (core/new-server-transaction! request))]
        (if (= "INVITE" (msg/method request))
          (let [dialog (core/new-dialog! server-transaction)]
            (and process-new-dialog (process-new-dialog dialog request server-transaction)))
          (and process-out-of-dialog-request (process-out-of-dialog-request request server-transaction))))
      (let [{process-dialog-confirmed :on-confirmed
             process-in-dialog-request :on-request} (dlg/application-data dialog)]
        (if (= (msg/method request) "ACK")
          (and process-dialog-confirmed (process-dialog-confirmed dialog request))
          (and process-in-dialog-request (process-in-dialog-request request dialog server-transaction)))))))

(defn- response-processor
  "Return a function to process response."
  {:added "0.2.0"}
  []
  (fn [{:keys [response client-transaction dialog]}]
    (when (not (nil? client-transaction))
      (let [{process-success :on-success
             process-failure :on-failure} (trans/application-data client-transaction)
            response-2xx? (= (quot (msg/status-code response) 100) 2)]
        (if response-2xx?
          (and process-success (process-success client-transaction response))
          (and process-failure (process-failure client-transaction response)))))))

(defn listener
  "Construct a new dum listener with several application layer event handlers."
  {:added "0.2.0"}
  [& event-handlers]
  {:pre [(core/already-bound-provider?)
         (even? (count event-handlers))
         (check-optional event-handlers :new-dialog fn?)
         (check-optional event-handlers :out-of-dialog-request fn?)]}
  (let [{:keys [new-dialog out-of-dialog-request]} (apply array-map event-handlers)]
    {:request (request-processor new-dialog out-of-dialog-request)
     :response (response-processor)}))

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
        alice (address (uri \"dream.com\" :user \"alice\") \"Alice\")]
    (send-request! \"MESSAGE\" :pack \"Welcome\" :to bob :from alice
      :use \"UDP\" :in :new-transaction :on-success #(prn %) :on-failure #(prn %)))

  If the pack content is not just a trivial string, provide a well named funciont
  to return a content map is recommended.
  {:type \"application\"
   :sub-type \"pidf-diff+xml\"
   :content content-object}"
  {:added "0.2.0"}
  [message & options]
  {:pre [(core/already-bound-provider?)
         (even? (count options))
         (check-optional options :pack legal-content?)
         (check-optional options :to addr/address?)
         (check-optional options :from addr/address?)
         (check-optional options :use :by upper-case in? ["UDP" "TCP"])
         (check-optional options :in #(or (= :new-dialog %) (dlg/dialog? %)))
         (check-optional options :more-headers vector?)
         (check-optional options :on-success fn?)
         (check-optional options :on-failure fn?)
         (check-optional options :on-request #(and (in? :new-dialog options) (fn? %)))
         (check-optional options :on-terminated #(and (in? :new-dialog options) (fn? %)))]}
  (let [{content :pack
         to-address :to
         from-address :from
         transport :use
         context :in
         more-headers :more-headers
         on-success :on-success
         on-failure :on-failure
         on-request :on-request
         on-terminated :on-terminated} (apply array-map options)
        {:keys [user domain display-name]} (account)
        {:keys [ip port transport]} (if (nil? transport)
                                      (core/listening-point)
                                      (core/listening-point transport))
        domain  (or domain ip)
        method  (upper-case (name message))]
    (if (dlg/dialog? context)
      (let [request (dlg/create-request context method)
            transaction (core/new-client-transcation! request)]
        (dlg/send-request! context transaction)
        request)
      (let [request-uri     (if (nil? to-address)
                              (throw (IllegalArgumentException. "Require the ':to' option for send an out of dialog request."))
                              (addr/uri-from-address to-address))
            from-address    (or from-address (addr/address (addr/sip-uri domain :user user) display-name))
            from-header     (header/from from-address (header/gen-tag))
            to-header       (if (= method "REGISTER") ; for REGISTER, To header's uri should equal the From header's.
                              (header/to (header/get-address from-header) nil) ; tag will be auto generated in transaction
                              (header/to to-address nil))
            contact-header  (header/contact (addr/address (addr/sip-uri ip :port port :transport transport :user user)))
            call-id-header  (core/gen-call-id-header)
            via-header      (header/via ip port transport nil) ; via branch will be auto generated before message sent.
            request         (msg/request method request-uri from-header call-id-header to-header via-header contact-header more-headers)
            transaction     (core/new-client-transcation! request)]
        (trans/set-application-data! transaction {:on-success on-success, :on-failure on-failure})
        (let [content-type        (or (:type content) "text")
              content-sub-type    (or (:sub-type content) "plain")
              content-type-header (header/content-type content-type content-sub-type)
              content             (or (:content content) content)]
          (msg/set-content! request content-type-header content))
        (if (= context :new-dialog)
          (let [dialog (core/new-dialog! transaction)]
            (dlg/set-application-data dialog {:on-request on-request, :on-terminated on-terminated}))
          (trans/send-request! transaction))
        request))))
