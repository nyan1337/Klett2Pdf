# Klett2Pdf
A small, quickly wrote (but working) Java CLI program that assembles a PDF file from a book on [Klett](https://klett.de). The results of this program are for private use only.

# Usage
## Requirements
* Java has to be installed and configured
* The URL of the ebook you want to download 
* A cookie from your browser. You can right-click in your browser and click Inspect. Go to the network tab, select a request and you should find the cookie in the request headers.

You can find a .jar file in the releases section.
[Download latest release](https://github.com/nyan1337/Klett2Pdf/releases/latest/Klett2Pdf.jar)

Open a terminal in the same directory the jar file is in.
Run the following command:
```
java -jar Klett2Pdf.jar
```
You will get help on which CLI arguments are required.


For Cornelsen books, there is also [Cornelsen2Pdf](https://github.com/nyan1337/Cornelsen2Pdf).
