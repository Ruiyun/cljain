(ns ^{:doc "place doc string here"
      :author "ruiyun"}
  cljain.header
  (:refer-clojure :exclude [replace reverse])
  (:use clojure.string
        [cljain.core :only [sip-factory]])
  (:import [javax.sip SipFactory]
           [javax.sip.header HeaderFactory]
           [gov.nist.javax.sip Utils]
           [java.lang Class]
           [java.lang.reflect Method]))

(def ^{:doc "place doc string here"
       :added "0.2.0"
       :private true}
  factory (.createHeaderFactory sip-factory))

(defn convert-name
  "place doc string here"
  {:added "0.2.0"}
  [java-name]
  (let [java-name (cond
                    (= java-name "createCSeqHeader") "createCseqHeader"
                    (= java-name "createRSeqHeader") "createRseqHeader"
                    (= java-name "createWWWAuthenticateHeader") "createWwwAuthenticateHeader"
                    (= java-name "createSIPIfMatchHeader") "createSipIfMatchHeader"
                    (= java-name "createSIPETagHeader") "createSipEtagHeader"
                    (= java-name "createRAckHeader") "createRackHeader"
                    (or (= java-name "Headers") (= java-name "Header")) java-name
                    :else (apply butlast java-name))]
        (subs (lower-case (replace java-name #"[A-Z][a-z]" #(str "-" %1))) 7)))

(defn header-create-methods
  "place doc string here"
  {:added "0.2.0"}
  []
  (map #(vector (convert-name (.getName %))) (.getMethods HeaderFactory)))


(defn gen-tag
  "Generate a new tag string."
  {:added "0.2.0"}
  []
  (.. Utils (getInstance) (generateTag)))

(defn gen-branch
  "Generate a new branch id string."
  {:added "0.2.0"}
  []
  (.. Utils (getInstance) (generateBranchId)))

(defmacro defheader
  "Use the macro to define sip headers. More document could be found here
  http://hudson.jboss.org/hudson/job/jain-sip/lastSuccessfulBuild/artifact/javadoc/index.html"
  {:arglists '([name docstring? attr-map? [args*]])
   :added "0.2.0"}
  [name & decl]
  (let [docstring (if (string? (first decl))
                    (first decl)
                    nil)
        decl      (if (string? (first decl))
                    (next decl)
                    decl)
        m         (if (map? (first decl))
                    (first decl)
                    {})
        args      (if (map? (first decl))
                   (next decl)
                   decl)
        args      (first args)
        m         (if docstring
                    (assoc m :doc docstring)
                    m)
        m         (if (meta name)
                    (conj (meta name) m)
                    m)
        m         (conj {:arglists (list 'quote (list args))} m)
        cls-name  (replace (capitalize name) #"-[a-z]" #(upper-case (subs %1 1)))
        cls-name  (str cls-name "Header")
        name      (symbol name)
        method    (symbol (str "create" cls-name))]
    `(do
       (def ~(with-meta name m) (partial (memfn ~method ~@args) ~factory)))))

(defheader accept
  "Creates a new AcceptEncodingHeader based on the newly supplied encoding value."
  [content-type sub-type])
;(defheader accept-encoding    encoding)
;(defheader accept-language    language)
;(defheader alert-info         alertInfo)
;(defheader c-seq              number method)
;(defheader from address tag)
;(defheader to address tag)
;(defheader via host port transport branch)
;(defheader max-forwards number)
;(defheader contact address)
;(defheader expires seconds)
;(defheader call-id id)
(defheader allow
  "Help me"
  {:added "0.2.0"}
  [method])

;(defheader content-type [type sub-type])

(defn extension
  "Creates a new Header based on the newly supplied name and value values."
  {:added "0.2.0"}
  [name value]
  (.createHeader factory name (str value)))
