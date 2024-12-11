# Schedule Checker
A simple tool written in Java that allows students at GMU to know whether any of their desired classes open up for registration. 
It will periodically check each requested class and alert the user once there is an opening. **This is inteded to be used for classes that do not have a waitlist.**

# Setup
Must have at least Java 17 or higher in order to run the program. ***You have to add the GMU website certificate to the Java keytool, which is described [here](#adding-the-certificatecertificate-not-added-to-keytool-warning-or-constantly-loading) or by running `addToKeytoolMac.command` or `addToKeytoolWindows.bat` as administrator.*** The certificate is needed in order for the program to make HTTP requests.

# How to Use
This tool can alert you when an opening is discovered for your classes. **You should launch the jar file through running `launchJarMac.command` or `launchJarWindows.bat`.**\
You first must check on the class scheduling website and then copy the class symbol, number, and section. If you would like keep tracking for any class regardless of section, you can leave that field empty.\
**You have to keep the program open in order for classes to be actively checked.**

## Adding the Certificate/"Certificate not added to keytool" Warning or Constantly Loading
This happens when the website certificate is not added to Java's keytool. You can add it by following the steps below:

### Getting the Certificate
**There is a certificate bundled with each release in case you do not want to manually download it. If you downloaded it through those means, skip to [Installing the Certificate](#installing-the-certificate)**

Navigate to this link in your browser:
https://ssbstureg.gmu.edu/StudentRegistrationSsb/ssb/registration \
Below are step-by-step instructions specific for your web browser in order to download the certificate onto your computer.

#### Chrome
1. Select the small options button that is to the left of the URL.
2. Click on "Connection is secure" and then "Certificate is valid"
3. A certificate viewer should open. Click on "Details"
4. By default, "ssbstureg.gmu.edu" should be selected. At the bottom click "Export..." *Make sure to change the file type to `.pem`. The file name should be saved as `ssbstureg-gmu-edu.pem`. You can save this into your downloads folder.

#### Firefox
1. Select the lock icon to the left of the URL.
2. Click on "Connection secure" and then "More information"
3. A new window should open, click on "View Certificate"
4. A new browser tab should open. Scroll down to **Miscellaneous**. Go to Download and select "PEM (cert)" The file name should be saved as `ssbstureg-gmu-edu.pem`. Save this into your downloads folder.

#### Safari
1. Select the lock icon left of the URL.
2. Click on "Show Certificate"
3. Click and drag the certificate icon and drag it to a finder window or the desktop.
4. Then drag into your downloads folder. The file should be renamed to `ssbstureg-gmu-edu.pem`.

### Installing the Certificate
Below are the commands if you downloaded it into your computer's "Downloads" folder. Make sure to change the `-storepass` argument to a different password if you changed the keystore password.

#### MacOS
Open a terminal window and copy and paste the following command.
```shell
cd Downloads &&
keytool -importcert -alias ClassScheduler -cacerts -file ssbstureg-gmu-edu.pem -storepass changeit
```

Then type "yes" (without quotations).
If there are no errors, you should get `Certificate was added to keystore`.
If that does not work, you can try and replace the keytool line with:

```shell
$JAVA_HOME/bin/keytool -importcert -alias ClassScheduler -cacerts -file ssbstureg-gmu-edu.pem -storepass changeit
```

#### Windows
Open an administrator command prompt/terminal (by searching for command prompt/terminal, right-clicking, and then selecting "Run as administrator") and copy and paste the following:

```shell
cd Downloads && keytool -importcert -alias ClassScheduler -cacerts -file ssbstureg-gmu-edu.pem -storepass changeit
```
Then type "yes" (without quotations).
If there are no errors, you should get `Certificate was added to keystore`.

## Removing the Certificate
If you are no longer using the tool, you can remove the certificate from the keytool with the following command or with `removeFromKeytoolMac.command` or `removeFromKeytoolWindows.bat` as administrator. Make sure to change the `-storepass` argument to a different password if you changed the keystore password.
```shell 
keytool -delete -alias ClassScheduler -cacerts -storepass changeit
```

# Building the Project
This Java project is built using Maven. The build targets Java 17.

# Open Source Dependencies
This program used the following dependencies:
- **MiGLayout** - Copyright 2024 Mikael Grev https://github.com/mikaelgrev/miglayout
- **JSON-java** - Public Domain https://github.com/stleary/JSON-java

# Icons
Status icons by https://icons8.com/