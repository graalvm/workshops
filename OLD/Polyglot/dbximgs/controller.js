module.exports.home = (req,res,next)=>{
  var fs = require('fs');

  // Location we will load the images from. These images will be
  // rendered into an image gallery, but we will also try to clasify
  // them first, using our tensor flow library.
  var path = './public/images';
  // Will hold out file objects
  var files = [];
  var classifier = null;

  // Now for the MAGIC...
  // This calls out to the java classes we compiled earlier
  // These are the classes that call to the TensorFlow lib and make use of the
  // linux shared libs
  var ClassiferClass = Java.type('org.tensorflow.examples.LabelImage');
  // Load the trained model, for the Java classifier
  var classifier = ClassiferClass.getClassifier('../models');

  // Find the files in the dir
  fs.readdir(path, function(err, items) {
    //
    for (var i=0; i<items.length; i++) {
        console.log(items[i]);
        var path = 'images/' + items[i];
        var javaImgPath = 'public/' + path;

        // The Java code returns a String description - which is turned automatically
        // into a JS string
        var clazz = 'Not using the Java lib to classify';
        var clazz = classifier == null ? path : classifier.classify(javaImgPath);

        files.push({path: path, title: clazz});
    }
    // Log that we have processed a file
    console.log(files);
  });

  // Render the Gallery page, pasing the file objects to it
  res.render('gallery', { imgs: files, layout:false});
};
