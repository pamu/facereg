# facereg
Face Recognition Service

To run this Application

1) Download the application
2) in the project directory run. "activator run"
3) Send RESTful request to the web service


for example

to access the service first get the user_key and then in the http header include the user_key

"user_key": "skdkk2khek32ke2kek"

then http request body should of the following format

# for training the face recognition model #

traget url for http request: http://domain:port/train or http://domain:port/smartTrain

{
"subjectName": "wolverine",
"galleryName": "xmen",
"image": "base64 String"
}

note: image should be in base64 format

reponse:

{"success": "training done"}


# for recognizing the subject #

traget url: http://domain:port/recognize

{
"galleryName": "xmen",
"image": "base64 String"
}

response body:

{
"success": "match",
"subjectName": "wolverince",
"confidence": "1.0"
}
