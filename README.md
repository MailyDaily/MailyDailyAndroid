# MailyDaily

MailyDaily is an Android app that integrates with Gmail to fetch unread emails, summarize them using AI, and suggest actionable items based on the email content. The app features a modern, user-friendly interface and includes Google Sign-In for user authentication.

## Features

- **Google Sign-In**: Allows users to log in with their Gmail account. ✅ 
- **Email Summarization**: Fetches unread emails and provides a summary.✅
- **Actionable Recommendations**: Generates actionable items from email content.✅
- **Modern UI**: User-friendly interface with profile display and action buttons.⏳
- **Conversations**: Allow user to request actions or clarifications for particular email❌
- **Actions**: Delete email, reply  ❌
- **Sign-In with other email clients**: Allows users to log in with accounts from various email clients.❌
- **Email Notifications**: Notify users of new unread emails / every morning or on set time . ❌
- **Voice Assistant Integration**: Allow users to interact with the app using voice commands. ❌
- **Settings Screen**: Manage app settings and preferences. ❌

## Current Progress 
![Alt text](https://github.com/mariankh1/MailyDailyAndroid/blob/version1/docs/assets/screenshots/1.png)
![Alt text](https://github.com/mariankh1/MailyDailyAndroid/blob/version1/docs/assets/screenshots/1.png)



## Technologies Used

- **Kotlin**: Programming language used for Android development.
- **Jetpack Compose**: UI toolkit for building native Android UIs.
- **Firebase**: Authentication and backend services.
- **Hugging Face API & MISTRAL MODEL**: For generating email summaries and recommendations.

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


## Running the Project

1. Open Android Studio and sync your project.
2. Connect an Android device or start an emulator.
3. Click Run to build and run the app.


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
