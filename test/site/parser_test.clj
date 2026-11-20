(ns site.parser-test
  (:require
    [site.core :as core]
    [site.parser :as parser]
    [clojure.test :as test :refer [is are deftest testing]]))

(def transform
  #(-> % parser/parse
     parser/transform
     :content))

(deftest test-headers
  (is (= [[:h1 "Hello"]]
        (parser/parse "# Hello")))
  
  (is (= [[:h1 "Hello"]
          [:h2 "Hello again"]]
        (parser/parse
          #ml "# Hello
               ## Hello again"))))

(deftest test-paragraphs
  (is (= [[:h1 "Hello"]
          [:p "Paragraph"]
          [:p "Paragraph 2"]]
        (parser/parse
          #ml "# Hello
               
               Paragraph
               
               Paragraph 2")))
  
  (is (= [[:h1 "Hello"]
          [:p "Paragraph"]
          [:p "Paragraph 2"]]
        (parser/parse
          #ml "# Hello
               Paragraph
               Paragraph 2")))
  
  (is (= [[:p "Paragraphs"]]
        (parser/parse 
          #ml "
               Paragraphs
               
               
               ")))
  
  (is (= [[:p "body body"]]
        (parser/parse "body body")))
  
  (is (= [[:p [:class "cls"] ".cls body body"]]
        (parser/parse ".cls .cls body body")))
  
  (is (= [[:p [:class "cls"] [:class "cls"] "body body"]]
        (parser/parse ".cls.cls body body")))
  
  (is (= [[:p [:class "cls"] [:class "cls"]]]
        (parser/parse ".cls.cls")))
  
  (is (= [[:p [:class "cls"] ".cls"]]
        (parser/parse ".cls .cls"))))

(deftest test-code-blocks
  (is (= [[:p "Some code:"]
          [:code-block "(+ 1 2)" "\n" "\n" "; => 3"]]
        (parser/parse
          #ml "Some code:
               
               ```
               (+ 1 2)
               
               ; => 3
               ```")))
  
  (is (= [[:p "Block 1:"]
          [:code-block "(+ 1 2)"]
          [:p "Block 2:"]
          [:code-block "(+ 3 4)"]]
        (parser/parse
          #ml "Block 1:
               
               ```
               (+ 1 2)
               ```
               
               Block 2:
               
               ```
               (+ 3 4)
               ```")))
  
  (is (= [[:code-block [:lang "clj"] "(+ 1 2)"]]
        (parser/parse
          #ml "```clj
               (+ 1 2)
               ```"))))

(deftest test-quote
  (is (= [[:p "Quote:"]
          [:blockquote
           [:p "abc " [:em "italic"] " " [:strong "bold"]]
           [:p "def"]]
          [:p "Another:"]
          [:blockquote
           [:p "quote"]]]
        (parser/parse
          #ml "Quote:
                  
               > abc *italic* **bold**
               > def
               
               Another:
               
               > quote"))))

(deftest test-figure
  (is (= [[:p "Image:"]
          [:figure "image.JpEg"]
          [:p "More text"]]
        (parser/parse
          #ml "Image:
               
               image.JpEg
               
               More text")))
  
  (is (= [[:p "Image:"]
          [:figure "image.JpEg"
           [:figcaption "caption with spaces"]]
          [:p "More text"]]
        (parser/parse
          #ml "Image:
               
               image.JpEg
               caption with spaces
               
               More text")))
  
  (is (= [[:figure "image.jpg"]]
        (parser/parse "image.jpg")))
  
  (is (= [[:figure "image.jpg"
           [:figlink "https://a.com"]]]
        (parser/parse "image.jpg https://a.com")))
  
  (is (= [[:figure "image.jpg"
           [:figalt "alt text"]]]
        (parser/parse "image.jpg alt text")))
  
  (is (= [[:figure "image.jpg"
           [:figcaption "caption"]]]
        (parser/parse "image.jpg\ncaption")))
  
  (is (= [[:figure "image.jpg"
           [:figlink "https://a.com"]
           [:figalt "alt text"]]]
        (parser/parse "image.jpg https://a.com alt text")))
  
  (is (= [[:figure "image.jpg"
           [:figlink "https://a.com"]
           [:figcaption "caption"]]]
        (parser/parse "image.jpg https://a.com\ncaption")))
    
  (is (= [[:figure "image.jpg"
           [:figalt "alt text"]
           [:figcaption "caption"]]]
        (parser/parse "image.jpg alt text\ncaption")))
  
  (is (= [[:figure "image.jpg"
           [:figlink "https://a.com"]
           [:figalt "alt text"]
           [:figcaption "caption"]]]
        (parser/parse "image.jpg https://a.com alt text\ncaption")))
  
  (is (= [[:figure
           [:a {:href "http://link"}
            [:img {:src "img.jpg"
                   :alt "alt text"
                   :title "alt text"
                   :loading "lazy"}]]
           [:figcaption
            "Also " [:em "some"] " caption"]]]
        (transform
          #ml "img.jpg http://link alt text
         Also *some* caption")))
  (is (= [[:figure [:img {:src "img@hover.jpg", :class "hoverable noround", :alt "alt text", :title "alt text", :loading "lazy"}] nil]]
        (transform ".noround img@hover.jpg alt text"))))

(deftest test-inlines
  (is (= [[:p "And " [:em "this"] " or " [:strong "this"] " or " [:code "this"]]]
        (parser/parse "And *this* or **this** or `this`")))
  
  (is (= [[:p [:em "em1"] " and " [:em "em2"]]]
        (parser/parse "*em1* and *em2*")))
  
  (is (= [[:p [:em "em1"] " and " [:em "em2"]]]
        (parser/parse "_em1_ and _em2_")))
  
  (is (= [[:p [:strong "em1"] " and " [:strong "em2"]]]
        (parser/parse "**em1** and **em2**")))
  
  (is (= [[:p [:strong "em1"] " and " [:strong "em2"]]]
        (parser/parse "__em1__ and __em2__")))
    
  (is (= [[:p "And " [:em "this"] " or " [:em "*" "this"] " or " [:code "this"]]]
        (parser/parse "And *this* or **this* or `this`")))

  (is (= [[:p "And " [:em "2" "*" "3"]]]
        (parser/parse "And *2*3*")))

  (is (= [[:p "And " [:em "2" "*" "3"]]]
        (parser/parse "And _2*3_")))

  (is (= [[:p "And " [:strong "2" "*" "3"]]]
        (parser/parse "And __2*3__")))

  (is (= [[:p "And " [:strong "_" "2" "*" "3" "_"]]]
        (parser/parse "And ___2*3___")))
  
  (is (= [[:p "Some " [:code "inline"] " code"]]
        (parser/parse "Some `inline` code")))
  
  (is (= [[:p "Some " [:code "escape \\` in code"] " code"]]
        (parser/parse "Some `escape \\` in code` code"))))

(deftest test-spaces-around-code
  (testing "code without spaces is untouched"
    (is (= [[:p "Some " [:code "inline"] " code"]]
          (transform "Some `inline` code")))

    (is (= [[:ul {:class "short-list"} [:li "Is " [:code "U+0041"] " here"]]]
          (transform "- Is `U+0041` here"))))

  (testing "code with spaces takes spaces around it"
    (is (= [[:p "Bytes" [:code " " "00 01 F4 A9" " "] "here"]]
          (transform "Bytes `00 01 F4 A9` here")))

    (is (= [[:p [:code "a b" " "] "and" [:code " " "c d"] "."]]
          (transform "`a b` and `c d`.")))

    (is (= [[:p [:code "a b"] "/" [:code "c d"] "," [:code " " "f(x, y)"]]]
          (transform "`a b`/`c d`, `f(x, y)`")))

    (is (= [[:p "(" [:code "x y"] ")"]]
          (transform "(`x y`)"))))

  (testing "one space between adjacent code"
    (is (= [[:p [:code "a b" " "] [:code "c d"]]]
          (transform "`a b` `c d`")))

    (is (= [[:p [:code "a"] [:code " " "c d"]]]
          (transform "`a` `c d`"))))

  (testing "only direct siblings"
    (is (= [[:p "In " [:em [:code "e m"]] " and " [:strong "see" [:code " " "x y"]]]]
          (transform "In *`e m`* and **see `x y`**"))))

  (testing "code blocks untouched"
    (is (= [[:p "Code:"] [:pre [:code "(+ 1 2)"]]]
          (transform "Code:\n\n```\n(+ 1 2)\n```")))))

(deftest test-escapes
  (is (= [[:p "Capturing " [:em "out"]]]
        (transform "Capturing *out*")))
  
  (is (= [[:p "Capturing *out*"]]
        (transform "Capturing \\*out*")))

  (is (= [[:p "Capturing ~280"]]
        (transform "Capturing \\~280")))

  (is (= [[:p "¯_(ツ)_/¯"]]
        (transform "¯\\_(ツ)_/¯")))

  (is (= [[:p "¯\\_(ツ)_/¯"]]
        (transform "¯\\\\\\_(ツ)_/¯"))))

(deftest test-detect-links
  (let [link [:a {:href "https://a.com/foo_bar_baz"} "a.com/foo_bar_baz"]]
    (testing "underscores in a URL do not break it"
      (is (= [[:p ["See " link " ok"]]]
            (transform "See https://a.com/foo_bar_baz ok")))

      (is (= [[:p [:em ["See " link " ok"]]]]
            (transform "*See https://a.com/foo_bar_baz ok*")))

      (is (= [[:p [:strong ["See " link " ok"]]]]
            (transform "**See https://a.com/foo_bar_baz ok**")))

      (is (= [[:p [:strong "See " [:em [link " ok"]]]]]
            (transform "**See *https://a.com/foo_bar_baz ok***"))))))

(deftest test-links
  (is (= [[:p "This "
           [:link
            [:alt "is a link"]
            [:href "https://abc"]]
           "."]]
        (parser/parse "This [is a link](https://abc).")))
  
  (is (= [[:p "This "
           [:link
            [:alt "is " [:strong "bold"] " link"]
            [:href "https://abc"]]
           "."]]
        (parser/parse "This [is **bold** link](https://abc)."))))

(deftest test-images
  (is (= [[:p "This " [:img [:alt "is an image"] [:href "https://abc"]] "."]]
        (parser/parse "This ![is an image](https://abc).")))
  
  (is (= [[:p [:img [:alt] [:href "https://abc"]]]]
        (parser/parse "![](https://abc)\n")))
  
  (is (= [[:p [:img [:alt] [:href]]]]
        (parser/parse "![]()"))))

(deftest test-html
  (is (= [[:p "Text "
           [:raw-html "<" "figure" ">" "Before " "<" "img" " src='...'" ">" " After" "</" "figure" ">"]
           " text"]]
        (parser/parse "Text <figure>Before <img src='...'> After</figure> text")))
  
  (is (= [[:p "Text " [:raw-html "<" "br" ">"] " text"]]
        (parser/parse "Text <br> text")))
  
  (is (= [[:raw-html "<" "center" ">" "text" "</" "center" ">"]]
        (parser/parse "<center>text</center>")))
  
  (is (= [[:raw-html "<" "center" ">" "text" "</ " "center" ">"]]
        (parser/parse "<center>text</ center>")))
  
  (is (= [[:raw-html "<" "img" " src='abc'" ">"]]
        (parser/parse "<img src='abc'>")))
  
  (is (= [[:raw-html "<" "img" " src='abc'" "/>"]]
        (parser/parse "<img src='abc'/>")))
  
  (is (= [[:raw-html "<" "img" " src='abc' " "/>"]]
        (parser/parse "<img src='abc' />"))))

(deftest test-lists
  (is (= [[:p "List:"]
          [:ul
           [:uli "a"]
           [:uli "b"]
           [:uli "c"]]
          [:p "Anyway..."]]
        (parser/parse
          #ml "List:
               
               - a
               - b
               - c
               
               Anyway...")))
  
  (is (= [[:p "List:"]
          [:ul
           [:uli "a"]
           [:uli "b"]
           [:uli "c"]]
          [:p "Anyway..."]]
        (parser/parse
          #ml "List:
               
               - a
               
               - b
               
               - c
               
               Anyway...")))
  
  (is (= [[:ul
           [:uli
            [:strong "strong"] " " [:em "em"] " just text"]
           [:uli
            [:code "code"]]]]
        (parser/parse
          #ml "- **strong** *em* just text
               - `code`")))
  
  (is (= [[:ol {:start "1" :class "short-list"}
           [:li "First"]
           [:li "Second"]
           [:li "Third"]]]
        (transform
          #ml "1. First
               2. Second
               3. Third")))
  
  (is (= [[:ol {:start "1" :class "short-list"}
           [:li "First"]
           [:li "Second"]
           [:li "Third"]]]
        (transform
          #ml "1. First
               
               2. Second
               
               3. Third")))
  
  (is (= [[:ol {:start "1" :class "short-list"}
           [:li "First"]
           [:li "Second"]
           [:li "Third"]]]
        (transform
          #ml "1. First
               1. Second
               1. Third")))
  
  (is (= [[:ol {:start "5" :class "short-list"}
           [:li "First"]
           [:li "Second"]
           [:li "Third"]]]
        (transform
          #ml "5. First
               6. Second
               7. Third")))
  
  (is (= [[:ol {:start "1" :class "short-list"}
           [:li "First"]]
          [:ul {:class "short-list"}
           [:li "Second"]]
          [:ol {:start "1" :class "short-list"}
           [:li "Third"]]]
        (transform
          #ml "1. First
               - Second
               1. Third"))))

(deftest test-short-lists
  (doseq [marker ["- " "5. "]]
    (testing (str "60-character boundary for " marker)
      (let [short-text (apply str (repeat 60 "a"))
            short-list (first (transform (str marker "Short\n" marker short-text)))
            long-list (first (transform (str marker "Short\n" marker short-text "a")))]
        (is (= "short-list" (:class (second short-list))))
        (is (not= "short-list" (:class (second long-list))))))
    (testing (str "inline markup and link URLs do not count for " marker)
      (let [text (apply str (repeat 60 "a"))
            url (str "https://example.com/" text)
            result (first (transform (str marker "**[" text "](" url ")**")))]
        (is (= "short-list" (:class (second result)))))))
  (testing "footnote lists keep their existing class"
    (is (= "footnotes" (-> (transform "[^1]: Short") first last second :class)))))

(deftest test-footnotes
  (is (= [[:p "Text" [:footnote "1"] " with" [:footnote "2"] " footnote" [:footnote "3"]]]
        (parser/parse
          "Text[^1] with[^2] footnote[^3]")))
  (is (= [[:fl
           [:fli "1" "First"]
           [:fli "2" "Second " [:em "em"] " " [:link [:alt "link"] [:href "href"]]]
           [:fli "3" "Third"]]]
        (parser/parse
          #ml "[^1]: First
         [^2]: Second *em* [link](href)
         
         [^3]: Third"))))

(comment
  (test/run-tests *ns*))
