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
* [For Windows](https://github.com/avritchie/tonga/releases/download/v0.1.2.3500/Tonga.Win.v0.1.2.3500.zip)  
* [For MacOS](https://github.com/avritchie/tonga/releases/download/v0.1.2.3500/Tonga.Mac.v0.1.2.3500.zip)  

*Make sure your computer has at least **Java version 9 or later** installed.*  
If not, first install the latest Java from:  
https://www.oracle.com/java/technologies/downloads/

#### How do I check my Java version?

* On Windows, open the command prompt (Start -> Windows System -> Command Prompt), write "java -version" and then press Enter.
* On MacOS, open the terminal (Finder -> Applications -> Utilities -> Terminal), write "java -version" and then press Enter.  

### Running

Open the Tonga.exe (Windows) or Tonga.app (MacOS) file located in the application folder.
* In case the exe/app file is blocked by antivirus protection or otherwise does not run, alternatively you can open the Tonga.bat (Windows) or Tonga.command (MacOS) file, which is also located in the application folder.
* If a MacOS computer gives you a message saying "To use the java command-line tool you need to install a JDK", please install the latest Java from: https://www.oracle.com/java/technologies/downloads/
* If you receive any other error, first ensure that your computer has at least Java 9 or later installed. Then check the log file for more details on what went wrong:
  * On Windows this is found at C:/Users/%USERNAME%/AppData/Local/Tonga/tonga.log
  * On MacOS this is found at ~/Library/Application Support/Tonga/tonga.log

## Usage

### User interface

The user interface consists of the left panel listing all the imported images ("Images") and their layers ("Layers"), a zoomable main image panel, a separate zoom image panel for viewing image details, and a right tabbed panel, containing filter and protocol parameters and execution, a result panel, settings, and a histogram panel. The bottom panel consists of a progress bar and a status bar, displaying status messages and brief explanations, when hovering the mouse over various UI components or menu options. Bottom right corner also shows the current xy-coordinates of the image (hovered by the mouse) and the zoom levels for the main panel and the zoom panel.

![](/images/screen/General.png)

### Images

Tonga is currently aimed for 2D fluorescent images, with most scientific image formats an general image formats in 8 or 16 bits per channel supported. Each image in Tonga can have one or more channels (called "layers"). Multi-channel stack images are supported, and three-dimensional Z-stack images are automatically converted into average intensity Z-projections. We highly discourage the usage of lossy formats, such as JPG, in scientific imaging and image analysis.

#### Importing

Images can be imported either from the menu (Tonga -> Import) or by simply dragging and dropping the files onto the user interface. The importing works differently depending on where the image was dropped.
* Dropping images on the image list ("Images") imports every file as a new image with one layer each.
* Dropping images on the main image panel import one new image with each file considered a layer of that image.
* Dropping images on the layer list ("Layers") import new layers to existing images. If the number of files dropped is divisible with the current number of images, the files are imported to all the images equally. If not, all the files are imported to the currently selected image as new layers.
An example: you have 3 images with one layer each and import 6 new images. Each image will get 2 new layers.

#### Viewing

The layers can be viewed separately or together as a "stack" image. In the stack mode, you can select the stack image rendering type from the "General" tab -> "Layer settings" -> "Stack mode". Clicking a layer during the stack mode excludes/includes it from the stack. In the separate (default) mode, pressing "G" on the keyboard will change the layer to partially transparent or opaque, indicated also by the background colour of the selected layer. Multiple layers can be selected by having the "Ctrl" or "Shift" button pressed while selecting. You can also render the currently selected layer/stack combination as a new layer by right clicking the layer list and selecting "Merge into one".

#### Histograms

![](/images/screen/Panel_5.png)

The histogram for the total brightness for the currently selected image is visible in the histogram panel. You can crop the display range using the sliders and selecting "Apply", and a new scaled copy of the current layer will be created. "Auto-adjust" automatically creates a scaled copy, with any unused ranges removed. Both of these can be applied to all images as well.  

Please note that for 8-bit images the whole range can be displayed at once, but for 16-bit images scaling may be necessary to make the image visible at all. However, scaling the values does not affect the underlying 16-bit image data, intensity measurements etc., and is only a visual change to the user.

#### Hotkeys and tips

* Spin the mouse wheel to zoom in/out in the zoom panel
* Keeping the Ctrl button pressed while scrolling zooms the main panel instead
* Move the zoomed main panel by dragging the image with the mouse
* Click the main panel to freeze or unfreeze the position of the zoom panel
* When the Tab key is pressed, the layer viewed previously is displayed temporarily
* Press Shift+Tab to select the layer viewed previously
* Double click a layer to select the same layer in all images (where possible)
* Press G to switch the layer being half transparent (on/off)
* Press S to switch the stack image mode on/off for this image
* Press Shift+S to switch the stack mode on/off for all images

### Image processing

The main functionality is achieved through three main components: filters, protocols, and counters.  
* Filters include general image processing tools, such as edge detection and morphological operations. Filters accept and produce one input and output image.  
* Protocols are combinations of various subsequently executed filters, additional supplementary code, and a counter. They provide a method for processing a series of images for a specific task, such as positive nuclei detection and extract numerical data. They accept and can produce one or more input and output images.  
* Counters are special functions for outputting only numerical data from the images into a results table.

The filter, protocols, and counters can be accessed and selected through the menu bar. Hovering the mouse over a menu item displays a brief description on the filter or protocol in question. The wizard feature (Protocols -> Launch the wizard! ✨) can also be used to select a protocol and set up protocol parameters by answering "yes or no" type of questions with example images and further explanations.

After selecting a filter/protocol/counter, it will show up at the tabbed panel, where parameters can be selected, and the action executed for the currently selected image only, or for all the images currently imported. After the processing is done, the output images (if any) will be appended to the channel list of the image processed. Numeric results from protocols or counters will be transferred into the result table found in the "Results" tab.

#### Protocol settings

![](/images/screen/Panel_1.png)

Each protocol may have one or more parameters to control the behaviour of the protocol. Each protocol also requires one or more input layers to be selected. In case multiple images will be processed, make sure that all the images have the selected layers in the same order (if you select the second layer in the settings as the input layer, the second one will be used for all images regardless of the layer name). It does not matter which layer(s) you have currently selected in the layer list on the left panel.

#### Filter settings

![](/images/screen/Panel_2.png)

The top section of the panel includes a shortcut to the available filters, by clicking the "..." button. The contents of this menu are indentical to the "Filters" menu at the menu bar. The combo control lists the previously used filters and saves their parameters for quick re-access.

The filter settings work with the same principle as protocol settings, except the input layer selection. Since each filter can only accept one layer as the input, the currently selected layer(s) in the layer list on the left panel will be used as the input. If multiple layers are selected, the filter will be separately executed on all of them. If the filter is executed on all images, make sure that all the images have the selected layers in the same order (if the second layer in the list is selected, the second one will be used for all images).

#### Results

![](/images/screen/Panel_4.png)

Any results produced by the protocols or counters will appear in the result table, where they can be sorted and edited. You can export the result table as a CSV file, or import them directly into Microsoft Excel, if installed.

#### Hotkeys and tips

* You can click the number of a slider control to change its value
* Press Ctrl+F to run the filter for the current image ("Run for single")
* Press Esc during filter or protocol execution to stop the execution

### General settings

![](/images/screen/Panel_3.png)

* **Alpha background** renders a background color for images which have transparency.
* **Stack mode** selects the method for rendering stack images in the stack mode.
  * *Sum* simply adds the values from the layers together
  * *Difference* subtracts the layer values from each other so that the difference between the layers is displayed
  * *Multiply* multiplies all the layer values with each other, so that white is considered 1, black 0, grey 0.5 etc.
  * *Maximum* always selects the highest value of all the layers for every pixel
  * *Minimum* always selects the lowest value of all the layers for every pixel
* **Multithreading** uses multiple CPU cores and threads to execute protocols for multiple images. May provide a large speed boost. Has no effect if only one image is processed.
* **Hardware rendering** uses GPU acceleration for rendering images in the user interface. Typically stack images etc. work faster this way. On some computers the GPU acceleration does not work and graphical glitches may be introduced. In these cases switching this off may help.
* **Work on disk** enables the batch mode, where the images are never imported to the software, until the processing has been started. Thus, you can not see the images in the user interface. This way, large number of images can be processed at once, without the need to import the data first, saving time and memory. The output images will be saved as PNG files, and the results will appear in the result table normally. This can only be enabled if you have not imported any images yet.
* **Append results** normally, the result table is cleared from old results every time a new protocol with new results is executed. If this is selected, the new results will be appended to the existing result table, if the columns match.
* **Autoscaling** when 16-bit images (especially microscopy image files with multiple images) are imported, the image may appear black because the whole 16-bit range is displayed. Often, the images need to be scaled for proper viewing. 
  * *None* does no scaling automatically.
  * *Image sets* when importing a file with multiple images, adjusts the scaling so that the differences between the images will stay comparable.
  * *Every image* scales every image separately for optimal viewing. Thus, differences between the images may not be comparable.
* **Aggressive** sometimes the images may have bright dots etc. affecting the scaling, and most of the data may still not be properly visible. This option ignores such minor areas.
* **File output** this is the output directory for any files exported from Tonga.
