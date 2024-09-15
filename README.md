# MailyDaily

MailyDaily is an Android app that integrates with Gmail to fetch unread emails, summarize them using AI, and suggest actionable items based on the email content. The app features a modern, user-friendly interface and includes Google Sign-In for user authentication.

## Features

- **Google Sign-In**: Allows users to log in with their Gmail account.
- **Email Summarization**: Fetches unread emails and provides a summary.
- **Actionable Recommendations**: Generates actionable items from email content.
- **Modern UI**: User-friendly interface with profile display and action buttons.

## Technologies Used

- **Kotlin**: Programming language used for Android development.
- **Jetpack Compose**: UI toolkit for building native Android UIs.
- **Firebase**: Authentication and backend services.
- **Hugging Face API**: For generating email summaries and recommendations.

## Prerequisites

- Android Studio
- A Firebase project
- A Hugging Face API key

## Setup Instructions

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/mailydaily.git
cd mailydaily
```

### 2.  Firebase Configuration

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
