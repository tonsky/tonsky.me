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
                   :title "alt text"}]]
           [:figcaption
            "Also " [:em "some"] " caption"]]]
        (transform
          #ml "img.jpg http://link alt text
         Also *some* caption")))
  (is (= [[:figure [:img {:src "img@hover.jpg", :class "hoverable noround", :alt "alt text", :title "alt text"}] nil]]
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

(deftest test-escapes
  (is (= [[:p "Capturing " [:em "out"]]]
        (transform "Capturing *out*")))
  
  (is (= [[:p "Capturing " "*" "out" "*"]]
        (transform "Capturing \\*out*")))
  
  (is (= [[:p "Capturing " "~" "280"]]
        (transform "Capturing \\~280")))
  
  (is (= [[:p "¯" "_" "(ツ)" "_" "/¯"]]
        (transform "¯\\_(ツ)_/¯")))
  
  (is (= [[:p "¯" "\\" "_" "(ツ)" "_" "/¯"]]
        (transform "¯\\\\\\_(ツ)_/¯"))))

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
  
  (is (= [[:ol {:start "1"}
           [:li "First"]
           [:li "Second"]
           [:li "Third"]]]
        (transform
          #ml "1. First
               2. Second
               3. Third")))
  
  (is (= [[:ol {:start "1"}
           [:li "First"]
           [:li "Second"]
           [:li "Third"]]]
        (transform
          #ml "1. First
               
               2. Second
               
               3. Third")))
  
  (is (= [[:ol {:start "1"}
           [:li "First"]
           [:li "Second"]
           [:li "Third"]]]
        (transform
          #ml "1. First
               1. Second
               1. Third")))
  
  (is (= [[:ol {:start "5"}
           [:li "First"]
           [:li "Second"]
           [:li "Third"]]]
        (transform
          #ml "5. First
               6. Second
               7. Third")))
  
  (is (= [[:ol {:start "1"}
           [:li "First"]]
          [:ul
           [:li "Second"]]
          [:ol {:start "1"}
           [:li "Third"]]]
        (transform
          #ml "1. First
               - Second
               1. Third"))))

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