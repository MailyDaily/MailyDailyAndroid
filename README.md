![version2](https://github.com/user-attachments/assets/a56f810a-a8d6-4c88-91b4-b7373c93268a)# MailyDaily

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
<p align="center">
  <img src="https://github.com/mariankh1/MailyDailyAndroid/blob/version1/docs/assets/screenshots/1.png" alt="Image 1" width="300"/>
  <img src="https://github.com/mariankh1/MailyDailyAndroid/blob/version1/docs/assets/screenshots/2.png" alt="Image 2" width="300"/>

  ![Alt Text](https://github.com/mariankh1/MailyDailyAndroid/blob/version1/docs/assets/screenshots/version2.gif)
</p>

## Technologies Used

- **Kotlin**: Programming language used for Android development.
- **Jetpack Compose**: UI toolkit for building native Android UIs.
- **Firebase**: Authentication and backend services.
- **Hugging Face API & MISTRAL MODEL**: For generating email summaries and recommendations.

## Prerequisites

- Android Studio
- A Hugging Face API key

## Setup Instructions

### 1. Clone the Repository

```bash
git clone https://github.com/MailyDaily/MailyDailyAndroid.git
cd mailydaily
```

### 2. Use your Hugging Face API key
At the moment the app is using a free API key from Hugging face. Please create and add your own key in the requests to avoid the maximum quota per hour.

## Running the Project

1. Open Android Studio and sync your project.
2. Connect an Android device or start an emulator.
3. Click Run to build and run the app.


## Contributing
Feel free to contribute to the project by submitting issues or pull requests. We welcome all contributions to **MailyDaily**!  
If you'd like to contribute, please follow these steps:

1. Fork the repository.
2. Create a new branch (`git checkout -b feature-branch`).
3. Make your changes and commit them (`git commit -am 'Add new feature'`).
4. Push to the branch (`git push origin feature-branch`).
5. Create a new Pull Request.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

Happy coding!
