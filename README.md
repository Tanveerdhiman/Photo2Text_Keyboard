# Photo2Text_Keyboard

**Photo2Text** - Photo2Text is an image recognition app which uses Optical Character Recognition (OCR) to detect text/ paragraphs from books and allows you to insert text into a document with a single tap on the desired paragraph.

App Demonstration Video - https://youtu.be/hg_qsNGKSjc

Unlike existing Text Detection apps like Google Lens, Where we first have to manually open up the app, 
Click an image, then manually select the desired paragraph, then copy the text and go back to the main document and paste it there,                                                                                                                               
Which is quite a tedious process especially if you working with multiple pages and paragraphs and you often have to go back n forth between the document and 
the Text Detection app.

On the other hand,

Photo2text works in conjunction with the device’s default input method (Keyboard), where we don’t have to open up any separate app, instead it opens up a small CameraView in the keyboard area itself where we can click an image of the desired paragraph and import the text straight into our document without going back n forth between different apps, Which save a lot of time.

                                To use Photo2Text - We follow a 3 step process
1.	Change the device’s input method to Photo2Text - We change the device’s default input method to Photo2Text by clicking on the Input Method Button at the bottom right in the device’s navigation bar.
2.	Capture the desired paragraph / page by pressing Recognize Button - On changing input method one will see a small CameraView in the keyboard area itself and we press on the Recognize Button to capture an image of the desired text from which we want to extract paragraphs from.
3.	Tap on the desired paragraphs / lines enclosed inside Red Boxes to insert Text into document - As soon as we press the recognize button, The app immediately detects whatever is on the page and it draws Red Boxes around Paragraphs or lines. 

To insert a paragraph into document, we just tap on a specific paragraph enclosed within red boxes and the tapped paragraph get inserted into the document.
Utility - Photo2Text saves a lot of time and allows one to quickly insert text from a physical book or document into your own document. It’s especially useful for students as students often have to make assignments where they often need to copy text from various books and notes to complete their assignment, and traditional apps like Google Lens etc. make things a bit complicated as one need to constantly switch between the document and Text detection app which wastes a lot of time.

And the best part is Its Completely Offline, You don’t need an internet connection to make the app work unlike existing apps like Google Lens where a  strong Internet connection is a major requirement.

Total Size - Very lightweight, The total size of the app is surprisingly just around 2-3 Mb, Which makes it very appealing for low end devices.

Framework, API & Programming Language used - The app is created with Java in Android Studio IDE.
I used Google ML Kit to implement the Text Detection Framework and used Camera2API to implement the Camera system. 

Backstory - 
The idea of the app came to my mind  back in 2019 when I was studying in 11th grade In high school and I was already working on a Text Detection Camera Based app,
It was exam time and I had no of assignment to submit and I was having a lot of trouble manually typing text from books and the existing apps like Google Lens were not that great and required a very strong internet connection.

As they say “Necessity Is The Mother of Invention” Out of frustration the idea of this app came to my mind and as I was already working on a similar app, 
it was quite easy for me to implement the code, So I made the first prototype of the app in 10 days in May 2019 which only supported basic text input method,

but I did not finished the app then as I got busy with other projects. After a year or so in 2021 I polished the app and made changes to it. 
I never published it on PlayStore as I feel It still needs some work, Especially Cross Device Compatibility and I got busier with more important projects.
