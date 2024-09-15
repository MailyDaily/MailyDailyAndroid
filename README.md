# MailyDaily

Welcome to **MailyDaily**, a React Native app designed to manage and summarize your emails with ease. This README will guide you through setting up and configuring Firebase for your project.

## Table of Contents

1. [Setup Instructions](#setup-instructions)
2. [Firebase Configuration](#firebase-configuration)
3. [Running the Project](#running-the-project)
4. [Contributing](#contributing)
5. [License](#license)

## Setup Instructions

1. **Clone the Repository**

    ```bash
    git clone https://github.com/yourusername/mailydaily.git
    cd mailydaily
    ```

2. **Install Dependencies**

    ```bash
    npm install
    ```

## Firebase Configuration

1. **Create a Firebase Project**

    - Go to the [Firebase Console](https://console.firebase.google.com/).
    - Click on **Add project** and follow the setup instructions.

2. **Add Your Android App to Firebase**

    - In the Firebase Console, select your project.
    - Click on the Android icon to add a new Android app.
    - Register your app with the package name `com.mariankh.mailydaily`.
    - Download the `google-services.json` file provided by Firebase.

3. **Add `google-services.json` to Your Project**

    - Place the `google-services.json` file in the `app` directory of your Android project (the same directory as `AndroidManifest.xml`).

4. **Configure Firebase in Your Project**

    - Open `build.gradle` (Project level) and ensure the Google services classpath is included:

      ```groovy
      buildscript {
          dependencies {
              classpath 'com.google.gms:google-services:4.3.15'
          }
      }
      ```

    - Open `build.gradle` (App level) and apply the Google services plugin at the bottom of the file:

      ```groovy
      apply plugin: 'com.android.application'
      apply plugin: 'com.google.gms.google-services'
      ```

    - Add Firebase dependencies:

      ```groovy
      dependencies {
          implementation 'com.google.firebase:firebase-auth:23.4.0'
          implementation 'com.google.firebase:firebase-firestore:24.7.0'
      }
      ```

5. **Sync Your Project with Gradle Files**

    - Click on **Sync Now** in Android Studio to sync your project with the updated Gradle files.

## Running the Project

1. **Start the Development Server**

    ```bash
    npm start
    ```

2. **Run the App**

    - For Android:

      ```bash
      npx react-native run-android
      ```

    - For iOS (macOS only):

      ```bash
      npx react-native run-ios
      ```

## Contributing

We welcome contributions to **MailyDaily**! If you'd like to contribute, please follow these steps:

1. Fork the repository.
2. Create a new branch (`git checkout -b feature-branch`).
3. Make your changes and commit them (`git commit -am 'Add new feature'`).
4. Push to the branch (`git push origin feature-branch`).
5. Create a new Pull Request.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

Happy coding!
