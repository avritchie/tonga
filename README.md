# Tonga

Bioimage analysis and segmentation. Nuclei detection and overlapping nucleus separation based on connecting concave points. Use protocols to count the number of nuclei and measure staining positivity and staining intensity. Usage via a complete user interface with no programming knowledge or setup required.

<img src="/images/screen/0_COMBINED.png" width="60%">

## Features

The protocols can be found and accessed through the protocol menu in the menu bar of the main user interface. Nuclei in the image will be detected, ovelapping or touching nuclei separated with concave point detection and connecting, and the intensity in the nucleus area measured when necessary. You can exclude nuclei which are touching the edges, and remove nuclei which appear dead or dividing.

#### Nuclei counting

(Protocols -> For nuclei -> Count the number of nuclei)  
Simply counts the number of nuclei in the image and reports the amount of nuclei.

![](/src/resourcePackage/wizard/7_0.png)

#### Nuclei intensity

(Protocols -> For nuclei -> Measure staining intensity)  
Does the same as above, but additionally uses the nucleus area to calculate the stain intensity. Reports the intensity as both the total intensity sum, and as an average intensity per object (%). You can also estimate the background intensity and have that subtracted from the results.

![](/src/resourcePackage/wizard/11_1.png)

#### Nuclei positivity

(Protocols -> For nuclei -> Count the ratio of positive nuclei)  
Does the same as above, but additionally classifies the nuclei as either positive or negative for the staining, using an intensity threshold configured in the parameters.

![](/src/resourcePackage/wizard/11_0.png)

#### Nuclei double intensity

(Protocols -> For nuclei -> Measure double-staining intensity)  
Does the same as "nuclei intensity", except multiplies the intensity with another dye. In other words, measures double-positivity for two dyes on separate layers. In completely other words, if a nucleus only has high intensity for dye 1 but not for dye 2, or high intensity for dye 2 but not for dye 1, its intensity measure is low/zero. Only if the intensity is high for both dyes, the intensity measure will also be high.

![](/src/resourcePackage/wizard/13_1.png)

#### Nuclei surrounding intensity

(Protocols -> For nuclei -> Measure surrounding intensity)  
After detecting the nuclei areas, uses a radius parameter to measure a dye intensity around the nuclei. Excludes the area of the nucleus itself.

![](/src/resourcePackage/wizard/12_1.png)


## Installing

<img src="/images/icons/SmallDrop.png" title="Our dad has a bigger microscope than your dad!" width="25%" align="right">

### Downloading

Please download and extract the latest compiled version:  
* [For Windows](https://github.com/avritchie/tonga/releases/download/v0.1.2.3707/Tonga.Win.v0.1.2.3707.zip)  
* [For MacOS](https://github.com/avritchie/tonga/releases/download/v0.1.2.3707/Tonga.Mac.v0.1.2.3707.zip)  

*Make sure your computer has at least **Java version 9 or later** installed.*  
If not, first install the latest Java from:  
https://www.oracle.com/java/technologies/downloads/

#### How do I check my Java version?

* On Windows, open the command prompt (Start -> Windows System -> Command Prompt), write "java -version" and then press Enter.
* On MacOS, open the terminal (Finder -> Applications -> Utilities -> Terminal), write "java -version" and then press Enter.  

### Running

#### Windows

Open the Tonga.exe file located in the application folder.
* In case the exe/app file is blocked by a group policy, an antivirus protection or otherwise does not run, alternatively you can open the Tonga.bat file, which is also located in the application folder.
* If Tonga starts, but gives an error, first ensure that your computer has at least Java 9 or later installed. Then check the log file for more details on what went wrong:
  * Click "Details" on the error message box to view the log file
  * The log file can be manually found at C:/Users/%USERNAME%/AppData/Local/Tonga/tonga.log

#### MacOS

Open the Tonga.app file located in the application folder. Due to MacOS restricting the usage of third-party applications (such as Tonga), issues may occur. See below for more instructions and solutions.
* You might get a warning saying that Tonga can not be opened because it is from an unidentified developer. In this case, right-click the Tonga.app and select "Open". If you get a message with the only option being "Cancel", right-click the Tonga.app and select "Open" for a second time, and a message box with an option "Open" should appear. Click this to run Tonga anyway.
* If MacOS gives you a message "The application "Tonga" can't be opened", or otherwise Tonga.app does not open, you can open the Tonga.command file, which is located in the same folder with the Tonga.app.
  * If you get a warning saying that the file can not be opened because it is from an unidentified developer, repeat the steps described above: right-click the Tonga.command and select "Open". If you get a message with the only option being "Cancel", right-click the Tonga.command and select "Open" for a second time, and a message box with an option "Open" should appear.
  * If you get an error saying "File could not be executed because you do not have appropriate access privileges", right-click Tonga.command while holding the Option-key from the keyboard, and select "Copy "Tonga.command" as pathname". Then open the terminal (Finder -> Applications -> Utilities -> Terminal), and write "chmod u+x" and the Spacebar. Right-click the window and select "Paste". Then press Enter. This command gives you the required access privileges to execute the file, and you can open the Tonga.command normally.
* If nothing described above works, right-click the Tonga.pre file (located in the same folder) while holding the Option-key from the keyboard, and select "Copy "Tonga.pre" as pathname". Then open the terminal (Finder -> Applications -> Utilities -> Terminal), and write "java -jar" and the Spacebar. Right-click the window and select "Paste". Then press Enter. This executes Tonga directly.
* If you get a message saying "To use the java command-line tool you need to install a JDK", please install the latest Java from: https://www.oracle.com/java/technologies/downloads/
* If Tonga starts, but gives an error, first ensure that your computer has at least Java 9 or later installed. Then check the log file for more details on what went wrong:
  * Click "Details" on the error message box to view the log file
  * The log file can be manually found at ~/Library/Application Support/Tonga/tonga.log

## Usage

Visit [the Tonga wiki](https://github.com/avritchie/tonga/wiki), where information about the usage of the software, including the available filters and protocols, can be found.