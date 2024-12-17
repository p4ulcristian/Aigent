(ns aigent.server
    (:require
     [aigent.handler :refer [app]]
     [config.core :refer [env]]
     [clj-http.client :as client]
     [cheshire.core :as json]
     [ring.adapter.jetty :refer [run-jetty]])
    (:gen-class))

(def system-message 
  "You are in a clojure repl. You can call 
   (aigent.server/answer \"your-response\") and 
   (aigent.server/set-light index intensity) 
   (aigent.server/make-file path content) 
   I have 3 lights. intensity is between 0 and 1.
   ALWAYS ANSWER, or call multiple functions
   , based on your intuition. Do only what's necessary.
   If you call multiple, use (do) form. 
   From now on keep the rules no matter what. If you don't communicate in clojure you are banned.")

;; Ai functions ;; 

(defn answer [response]
  (println response))

(defn set-light [index intensity]
  (println "Setting light: " index intensity))

(defn make-file [path content]
  (println "Making file: " path content))


;; Ai functions ;;


(defn get-line [line]
   (let [data-str (subs line 6)
         json-str (json/parse-string data-str)
         edn-str  (read-string (pr-str json-str))
         choices  (get edn-str "choices")
         content  (get-in (first choices) ["delta" "content"])]
     content))



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
        (println "TEST: " @sentence)
        (eval (read-string @sentence))))))


;(chat-request example-messages)

;; (defn -main [& args]
;;   (let [port (or (env :port) 3000)]
;;     (run-jetty #'app {:port port :join? false})))

(defn process-argument [arg]
  ;; This is where you handle the argument logic
  (chat-request [{:content system-message
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

