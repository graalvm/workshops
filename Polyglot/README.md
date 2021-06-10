# Polyglot Apps on GraalVM EE

## Introduction

This demo / workshop will demonstrate how GraalVM let's you easily combine different
programming languages into a single applicaiton. It will highlight the integration between
Java, JS and R.

It's built form a number of parts, that we will stick together durring the course of this workshop.

The first part is a Java command line application that classifies images, admittidley not very well,
using a tensor flow model. I've extended the command line app classes so that it can also be called
as a Java library.

The seocnd part is a simple Express JS node web application. This loads a directory of
images and renders them as a gallery. Nothing too special there.

The third part is where we combine these two separare applications. We will add the Java classifier
functionality to the Express node app - with suprising ease.

## Props

Before we begin I need to include links to the repos that this has been built from:

1) https://dropbox.github.io/nodegallerytutorial/

2) https://raw.githubusercontent.com/tensorflow/tensorflow/master/tensorflow/java/src/main/java/org/tensorflow/examples/LabelImage.java

## Install Tensor Flow Java

Tensorflow is a machine learning library. We will be using it, along with a pre-trained model, to
clasify our images.

Read the instructions on this page to get the correct setup for your OS:

https://www.tensorflow.org/install/lang_java

When you have downloaded the tar file for your OS, extract it into the `/jni`
folder.

## Java Application

The steps to build and run the classifier command line app are pretty easy, once the
install of the tensorflow libs is done.

To Build:

``` shell
mvn clean package
```

When running it is worth taking a quick look at the run script. Note thet we have to
tell Java abou tthe shared libs:

`-Djava.library.path=./jni`

To Run:

``` shell
./run.sh dbximgs/public/images/cat-01.jpeg
```

If all goes well, and tensorflow is correctly ihstalled, then you will now have a, possibly
poor!, classification of the image we passed to the app. Maybe take a look at the image and see if
the classification was any good?

## Node App

The node / Express app is fairly straight-forward.

You don't need to build anything, just make sure that the Java app is built first. It consists of
a main server js, some routes, a controller and some templates. The main logic of interest to us
will be in the `controller.js`.

But first, let's run the app.

To run:

```
$ cd dbximgs
$ ./run-node.sh
```

This should start up a web site that you can access on : [http://localhost:3000](http://localhost:3000)

## Combining the Java Command Line Application, Easily, with Our Node App

So now we have two things. How we do combine them? First, let us look at the Java command line app.
We need to make a simple change to that that will allow it to be used as a "library".

Within the file, `LabelImage.java`, look at the following:

```
    /**
     * Allows the class to be used as a library within other application. All they
     * needd to do is to use the static getClassifier() method to create an instance of
     * the classifier.
     * @param modelDir
     * @return
     */
    public static LabelImage getClassifier(final String modelDir) {
        final LabelImage classifier = new LabelImage(modelDir);
        return classifier;
    }
```

This method, as the docs suggest, allows another application to create an instance of the class.

Next, we are going to look at how we can make this Java class, and other classes, available to
the node graalvm instance that runs our app. So, let's take a look at the scrip that launches our
node app, `dbximgs/run-node.sh`

```
#node --jvm --vm.cp=../jars/libtensorflow-1.14.0.jar:../target/classes --vm.Djava.library.path=../jni ./bin/www
node ./bin/www
```

So if we **uncomment** the first line and **comment** out the second line. Let's take a look at
the new line that now runs our node app:

* `--jvm` : This tells the GraalVM node instance to run in non-native mode, so there will be full compatability with JAVA
* `--vm.cp` : We can pass Java jars, classes that we want to be available to Node and our custom code  using this.
* `--vm.Djava.library` : We can pass parameters to the underlying JVM using `--vm.D` parameters. Here we need to tell it about the native libaries etc that are needed by tensorflow

We can find out the full details on all the parameters by running:

```
node --help
node --help:expert
```

So let's check agian that our app works. Cool? All good.

Next we need to see how to access our custom Java object from within the node Javascript.
GraalVm makes this incredibly easy. Take a look at the following code snippet from
`controller.js`:

```
// Now for the magic...
  // This calls out to the java classes we compiled earlier
  // These are the classes that call to the TensorFlow lib and make use of the
  // linux shared libs
  //var ClassiferClass = Java.type('org.tensorflow.examples.LabelImage');
  //var classifier = ClassiferClass.getClassifier('../models');
```

If we uncomment out the two commented lines we can see that we are creating
a native Java tyle, a class, directly in our Javascript using the `Java`
implicit object. This is added by GraalVM.

Once we have the type we can call it's methods and the types returned by it
are directly usable within our code. Nice.

Uncommenting the above lines brings the classifier into out code. We now need
to use it. Look for the following code within `controller.js`:

```
        // The Java code returns a String description - which is turned automatically
        // into a JS string
        //var clazz = classifier == null ? path : classifier.classify(javaImgPath);
        var clazz = "A Thing";
```

Again uncomment the first line out and comment out the second. The following line
calls the classifier by passing in the path to the image (note the path is relative
to where the node binary is run fgrom). It just returns a String, that JS knows how
to use.

```
        var clazz = classifier == null ? path : classifier.classify(javaImgPath);
```

Now run the app again - what happens? We should see that the generic message for the pictures in
the gallery is now replaced with an actual classification.

## Make the App Truly Polyglot!

## Conclusion

Hopefully we have seen that interopation from Javascript to Java is easy. At the moment
you can't really inspect the Java objects / classes that you bring into a JS program,
but that may come.