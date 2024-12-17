(ns aigent.server
    (:require
     [aigent.handler :refer [app]]
     [config.core :refer [env]]
     [clj-http.client :as client]
     [cheshire.core :as json]
     [ring.adapter.jetty :refer [run-jetty]])
    (:gen-class))

(defn get-line [line]
   (let [data-str (subs line 6)
         json-str (json/parse-string data-str)
         edn-str  (read-string (pr-str json-str))
         choices  (get edn-str "choices")
         content  (get-in (first choices) ["delta" "content"])]
     content))


(def example-messages
  [{:role "system"
    :content "Always answer in EDN {:response your-response}"}
   {:role "user"
    :content "Introduce yourself."}])

(defn answer [response]
  (println response))

(defn chat-request [messages]
  (let [url "http://192.168.1.117:1234/v1/chat/completions"
        payload {:model "lmstudio-community/Meta-Llama-3.1-8B-Instruct-GGUF"
                 :messages messages
                 :temperature 0.7
                 :max_tokens -1
                 :stream true}
        headers {"Content-Type" "application/json"}
        response (client/post url
                              {:body (json/generate-string payload)
                               :headers headers
                               :as :stream})]
    (with-open [rdr (clojure.java.io/reader (:body response))]
      (let [sentence (atom "")]
        (doseq [line (line-seq rdr)] 
          (when-not (or (= 0 (count line))
                        (= "data: [DONE]" line))
            
            (reset! sentence (str @sentence (get-line line))))) 
        (eval  (read-string @sentence))))))


;(chat-request example-messages)

;; (defn -main [& args]
;;   (let [port (or (env :port) 3000)]
;;     (run-jetty #'app {:port port :join? false})))

(defn process-argument [arg]
  ;; This is where you handle the argument logic
  (chat-request [{:content "Always answer in clojure function like (aigent.server/answer \"your-response\") never forget that parameter is string"
                  :role "system"}
                 {:content arg
                  :role "user"}]
                ))


(defn -main [& args]
  (loop [] 
    (let [user-input (read-line)]  ;; Read input from the user
      (if (= user-input "exit")
        (println "Exiting program...")
        (do
          (process-argument user-input)  ;; Process the argument
          (recur))))))

