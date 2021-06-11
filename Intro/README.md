# Install and Setup GraalVM Enterprise Edition

This workshow will take you through downloading and installing the most recent GraalVM Enterprise, **21.1.0**, and adding support for its accompanying features and languages runtimes.

## Downloading GraalVM Enterprise Edition 21.1.0

It is important to state that are there are two versions of `GraalVM`, the Enterprise Edition (supported and with better performance) and the Community Edition (free and open source). Both of these can be dwonloaded from the GraalVM website, but this workshop will detail downloading, and then installing the Enterprise Edition.

1. Navigate to [Oracle GraalVM Downloads](https://www.oracle.com/downloads/graalvm-downloads.html?selected_tab=21).

2. Click on the Current Release tab on the page to display the download links for the latest release containing new features. Make sure that you select 21.1.0 (the default should be selected already).

3. Select the JDK version (8, 11 or 16) and the operating system (Linux, macOS, Windows).

   ![GraalVM Download Page - Select Current Verion, JDK and OS](./images/download-page.png " ")

   <!-- [Description of asset](./files/download-page.txt) -->

4. Click on the Oracle GraalVM Enterprise Edition Core download link.

5. Before the download starts, you must accept the [Oracle License Agreement](https://www.oracle.com/downloads/licenses/graalvm-otn-license.html), license that allows you to use GraalVM Enterprise for evaulation and devleopment purposes, in the popup window. It will then ask you to log into Oracle. If you have an Oracle OTN account then use this identity to download GraalVM. If not, create one by hitting the "Create Acount" button at the bottom opf the page.

6. When the download button becomes active, press it to start downloading.

You see other plugins available for downloading down the page. These are GraalVM technologies that can be optionally installed, for example, "Oracle GraalVM Enterprise Edition Java on Truffle", "GraalVM LLVM Toolchain Plugin" etc. You don't have to download them. Later in the lab we will show how to install them from the command line.

## Installing GraalVM Enterprise

Depending on the operating system, Linux, macOS or Windows, some steps may differ. Please follow commands for your platform.

### Linux

1. Move the _archive.tar.gz_ you downloaded at the previous setion to the directory where you want to install GraalVM Enterprise.

2. Open the terminal (command prompt) and unzip the archive:

   ```shell
   tar -xzf archive.tar.gz
   ```
3. Configure the runtime environment, as there can be multiple JDKs installed on the machine:

      - Point the `PATH` environment variable to the GraalVM Enterprise `bin` directory:
        ```shell
        export PATH=/path/to/<graalvm>/bin:$PATH
        ```
      - Set the `JAVA_HOME` environment variable to resolve to the installation directory:
        ```shell
        export JAVA_HOME=/path/to/<graalvm>
        ```
 4. Run the `java -version` command to check whether the installation was successful.

### macOS

1. Open the terminal and change to the location where you donwnloded the _archive.tar.gz_ (by default, it will be `Users/user_name/Downloads`):

2. Unzip the archive:

    ```shell
    tar -xzf archive.tar.gz
    ```
    Alternatively, open the file in Finder.

3. Move the downloaded package to its proper location, the `/Library/Java/JavaVirtualMachines` directory. Since this is a system directory, `sudo` is required:

    ```shell
    sudo mv graalvm-ee-java<version>-<version> /Library/Java/JavaVirtualMachines
    ```
4. Run `/usr/libexec/java_home -V` to verify if the move is successful and to get a list of all installed JDKs:

    ```shell
    /usr/libexec/java_home -V
    ```

5. Configure the runtime environment, as there can be multiple JDKs installed on the machine:

    - Point the `PATH` environment variable to the GraalVM Enterprise `bin` directory:
    ```shell
    export PATH=/Library/Java/JavaVirtualMachines/<graalvm>/Contents/Home/bin:$PATH
    ```
    - Set the `JAVA_HOME` environment variable to resolve to the GraalVM Enterprise installation directory:
    ```shell
    export JAVA_HOME=/Library/Java/JavaVirtualMachines/<graalvm>/Contents/Home
    ```

#### Notes on Setting up GraalVM on macOS

A number of the details will vary a little from the general approach.

First, if you place GraalVM to `/Library/Java/JavaVirtualMachines/` location, macOS picks This JDK up as default (as long it is the newest JDK in the directory). You may not want to do this.

Second, the path that the core GraalVM extracts to is different from that for Linux or Windows. On macOS, the
directory for GraalVM contains extra directories as your `GRAALVM_HOME`: `<install-dir>/<graalvm-ee>/Contents/Home`.

The third major difference is that on some versions of macOS (Catalina and later ) the `GateKeeper` service will block you from running `GraalVM` as it is not a signed application / binary. This can be worked around in a number of ways:

- Remove the quarantine attribute from the binary:

  ```shell
  sudo xattr -r -d com.apple.quarantine /path/to/GRAALVM_HOME
  ```
- Disabling `GateKeeper`, running and then reenabling.
- On latter versions of macOS, you will get prompted that it is blocked and `GateKeeper` will prompt you to add an exception for it.
- You can add make a command line app allowed by using the `spctl` tool:

  ```shell
  # Run from the terminal
  # This assumes that you install GraalVM in /Library/Java/...
  # Update if you choose to install in another location
  sudo spctl add $GRAALVM/bin/java
  ```

Here is the configuration example from `~/.bash-profile`:
  ~~~ {.bash}
  # GraalVM Enterprise based on JDK11
  export GRAALVM_HOME=~/bin/graal/graalvm-ee-java11-21.1.0/Contents/Home
  export JAVA_HOME=${GRAALVM_HOME}
  export PATH=${GRAALVM_HOME}/bin:$PATH
  ~~~

### Windows

1. Change the directory to the location where you want to install GraalVM Enterprise, then move the _.zip_ archive to it.

2. Unzip the archive to your file system.

3. Configure the runtime environment, as there can be multiple JDKs installed on the machine. Setting environment variables via the command line will work the same way for Windows 7, 8 and 10.

      - Point the `PATH` environment variable to the GraalVM Enterprise `bin` directory:
        ```shell
        setx /M PATH "C:\Progra~1\Java\<graalvm>\bin;%PATH%"
        ```
      - Set the `JAVA_HOME` environment variable to resolve to the GraalVM Enterprise installation directory:
        ```shell
        setx /M JAVA_HOME "C:\Progra~1\Java\<graalvm>"
        ```
      Note that the `/M` flag, equivalent to `-m`, requires elevated user privileges.

### Testing Your Installation

1. Open a new terminal window / command prompt and run `java -version` to check whether the installation was successful. The output should match the following:

    ```shell
    java -version
    java version "1.8.0_261"
    Java(TM) SE Runtime Environment (build 1.8.0_261-b33)
    Java HotSpot(TM) 64-Bit Server VM GraalVM EE 21.1.0 (build 25.261-b33-jvmci-20.1-b04, mixed mode)
    ```
    
2. Test out if the GraalVM Updater tool is available:

    ```shell
    gu --help
    ```

Did that work? If it did you now have access to the package tool that will allow you to install the various additional technologies that come with GraalVM. We will go through a few of these shortly.

## Installing Languages Runtimes

GraalVM provides the `gu` package tool to install additional technologies, such as the various language runtimes. In this section we will step through installing these.

All language / components installations are carried out using the `gu` tool that is distributed with GraalVM. The latest instructions on using this toll to install components can be found [here](https://docs.oracle.com/en/graalvm/enterprise/21/docs/reference-manual/graalvm-updater/).

### Installing Node.js Runtime (Optional)

GraalVM Enterprise can execute plain JavaScript code, both in REPL mode and by executing script files directly:
  ```shell
  js -version
  ```

GraalVM Enterprise also supports running Node.js applications, but you need to install the Node.js support:
  ```shell
  gu install nodejs
  node -v
  v14.16.1
  ```
If you have `node` already installed, you may need to change your path or explicitly specify the path to the GraalVM version of `node`.

### Installing LLVM Toolchain (Optional)

The GraalVM LLVM runtime can execute C/C++, Rust, and other programming language that can be compiled to LLVM bitcode.
A native program has to be compiled to LLVM bitcode using an LLVM frontend such as `clang`.
The C/C++ code can be compiled to LLVM bitcode using `clang` shipped with GraalVM via a prebuilt LLVM toolchain.

Also, a number of the supported languages runtimes require the `llvm-toolchain` in order to work. Regardless of whether you are using the Enterprise or Community Edition, you will need to install the same version of this.

1. This is installed simply, using the `gu` command:

      ```shell
      gu install llvm-toolchain
      ```
2. Additionally, you may export the `LLVM_TOOLCHAIN` variable to the toolchain location for convenience:

      ```shell
      export LLVM_TOOLCHAIN=$(lli --print-toolchain-path)
      ```

### Installing Python Runtime (Optional)

The Python support can be installed simply, using the `gu` command:

  ```shell
  gu install python
  ```
Once it is installed, the `graalpython` launcher will become available to run Python programs with GraalLVM.

### Installing Ruby Runtime (Optional)

The Ruby support can be installed simply, using the `gu` command:

  ```shell
  gu install ruby
  ```
Once it is installed, Ruby launchers like `ruby`, `gem`, `irb`, `rake`, `rdoc`, and `ri` become available to run Ruby programs with GraalLVM.

### Installing R Runtime (Optional)

GraalVM provides a GNU-compatible environment to run R programs directly or in the REPL mode.
To install the runtime for R, run:

  ```shell
  gu install r
  ```
Then the `R` and `Rscript` launchers become available.

### Installing WebAssembly Runtime (Optional)

With GraalVM you can run programs compiled to WebAssembly.
To install the support for WebAssembly, run:

  ```shell
  gu install wasm
  ```
Then the `wasm` launcher, that can run compiled WebAssembly binary code, becomes available.

## Installing Additional Features

### Installing Native Image (Optional)

Using the Native Image technology offered by GraalVM, you can compile ahead-of-time Java bytecode into a platform-specific, self-contained, native executable to achieve faster startup and a smaller footprint for your application.

1. Install support for ahead-of-time compilation (AOT:

      ```shell
      gu install native-image
      ```

2. Test if the installation was successful:

      ```shell
      native-image --version
      ```
3. Ensure you [meet the prerequisites](https://docs.oracle.com/en/graalvm/enterprise/21/docs/reference-manual/enterprise-native-image#prerequisites) (you have necessary libs, like `glibc-devel, zlib-devel`,  available on your system). Please note, the prerequisites for using Native Image on Windows differ.

Full instructions can be found [here](https://docs.oracle.com/en/graalvm/enterprise/21/docs/reference-manual/enterprise-native-image/).

### Installing Java on Truffle (Optional)

[do we need it?]

## Related Links

### Visual Studio Code

A number of plugins have been written for Visual Studio Code (VS Code) that allow for better integration with the GraalVM eco-system. Proceed to [this guide to learn about GraalVM intergation into VS Code](https://www.graalvm.org/tools/vscode/).

You are more than welcome to use another editor. When it comes to the polyglot debugging, please make sure that you have the Google Chrome browser installed.

### Docker - Pre-built Docker Images

To support container-based development, GraalVM is available on a number of pre-built Docker images.
Currently only the images of GraalVM Community Edition are available for pulling from the [GitHub Container Registry](https://github.com/orgs/graalvm/packages/container/package/graalvm-ce), but we will be making the GraalVM Enterprise images available in Oracle Container Registry soon. For now, you can learn [how to start using GraalVM Community images for Docker containers](https://www.graalvm.org/docs/getting-started/container-images/).

### Other related resources:

- [Get Started with GraalVM Enterprise](https://docs.oracle.com/en/graalvm/enterprise/21/docs/getting-started/)
- [Get Started with GraalVM Enterprise on OCI](https://docs.oracle.com/en/graalvm/enterprise/21/docs/getting-started/oci/compute-instances/)
- [Get started with GraalVM on Ampere A1 on Oracle Cloud Infrastructure](https://docs.oracle.com/en/learn/oci_graalvm_ampere_a1/index.html)
