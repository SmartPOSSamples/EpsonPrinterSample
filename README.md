# EpsonPrinterSample

###### See Issues if you have questions.

CSDN [EPSON Printer Printer Sdk For Android](https://blog.csdn.net/a23006239/article/details/78871913)

##### Steps for EPOSN SDK printing

- 1 First allow the printer device to be discovered, and a callback listens to obtain the TargetId of the connected printer device afterwards.

- 2 Initialize the Printer print class and set up print listening.

- 3 Create print data to add to the created to Printer object, where the small ticket font styles are set.

- 4 Connect to the printer device and sends it to the command buffer for printing via the sendData() method.

- 5 Finish printing, clear the command buffer, and close the release print object.

![image.png](http://upload-images.jianshu.io/upload_images/956862-ebab845196241808.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)
