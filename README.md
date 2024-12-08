# Schedule Checker
A simple tool written in Java that allows students at GMU to know whether any of their desired classes open up for registration. 
It will periodically check each requested class and alert the user once there is an opening. **This is inteded to be used for classes that do not have a waitlist.**

# Setup
Must have at least Java 17 or higher in order to run the program. ***You have to add the GMU website certificate to the Java keytool, which is decribed below.***
## Adding the Certificate/"Certificate not added to keytool" Warning
This happens when the website certificate is not added to Java's keytool. You can add it by following the steps below:
### Getting the Certificate
**There is a certificate bundled with each release in case you do not want to manually download it. If you downloaded it through those means, skip to [Installing the Certificate](#installing-the-certificate)**\

Navigate to this link in your browser:
https://ssbstureg.gmu.edu/StudentRegistrationSsb/ssb/registration \
Below are step-by-step instructions specific for your web browser in order to download the certificate onto your computer.
#### Chrome
1. Select the small options button that is to the left of the URL.
2. Click on "Connection is secure" and then "Certificate is valid"
3. A certificate viewer should open. Click on "Details"
4. By default, "ssbstureg.gmu.edu" should be selected. At the bottom click "Export..." You can save this into your downloads folder.
#### Firefox
1. Select the lock icon to the left of the URL.
2. Click on "Connection secure" and then "More information"
3. A new window should open, click on "View Certificate"
4. A new browser tab should open. Scroll down to **Miscellaneous**. Go to Download and select "PEM (cert)" Save this into your downloads folder.
#### Safari
1. Select the lock icon left of the URL.
2. Click on "Show Certificate"
3. Click and drag the certificate icon and drag it to a finder window or the desktop.
### Installing the Certificate
After you have downloaded the certificate, you must must open a terminal window and CD to your folder that you downloaded the file. Below are the commands if you downloaded it into your computer's "Downloads" folder. Make sure to change the `-storepass` argument to a different password if you changed the keystore password.\
**Copy each line and run one at a time.**
```shell
cd Downloads
$JAVA_HOME/bin/keytool -importcert -alias GMUScheduler -cacerts -file ssbstureg-gmu-edu.pem -storepass changeit
yes
```
If there are no errors, you should get `Certificate was added to keystore`.
## Removing the Certificate
If you are no longer using the tool, you can remove the certificate from the keytool with the following command. Make sure to change the `-storepass` argument to a different password if you changed the keystore password.
```shell 
keytool -delete -alias GMUScheduler -cacerts -storepass changeit
```
# Building
This Java project is built using Maven. The build targets Java 17.
# Open Source Dependencies
This program used the following dependencies:
- **MiGLayout** - Copyright 2024 Mikael Grev https://github.com/mikaelgrev/miglayout
- **JSON-java** - Public Domain https://github.com/stleary/JSON-java
# Icons
Status icons by https://icons8.com/