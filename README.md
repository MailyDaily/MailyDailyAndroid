#MailyDaily

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


## Contributing to MailyDaily

We’re excited to welcome contributions to MailyDaily! Whether you’ve found a bug, have an idea for a new feature, or want to enhance the project, your help is appreciated. Follow the guide below to get started.

### How to Contribute

1. **Fork the Repository**  

    First, fork the repository by clicking the "Fork" button at the top-right of this page. This creates your own copy of the repository where you can make changes.

3. **Clone Your Fork**  

    After forking, clone your forked repository to your local machine:
   ```bash
   git clone https://github.com/your-username/MailyDaily.git
   cd MailyDaily
   ```
4. **Create a New Branch**

    It’s best to create a new branch to keep your work separate from the main project:
bash
```bash
  git checkout -b feature-branch
```
 Replace feature-branch with a descriptive name (e.g., add-login-feature or fix-typo-in-readme).
 
4. **Make Your Changes**

    Now you can make your changes to the code or documentation.
   
6.  **Commit Your Changes**

 After making changes, commit them with a descriptive message:
```bash
git commit -am "Add new login feature"
```

6.  **Push Your Changes**

Push your changes to your forked repository on GitHub:
```bash
git push origin feature-branch
```
7.  **Create a Pull Request (PR)**
   
   Go back to the original repository (where you forked from) and create a pull request. You’ll see an option to compare your branch with the original one. Add a detailed description of what you’ve changed or added.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.


### Acknowledgements

This project is currently maintained by **[Marian Kh](https://github.com/mariankh1)** and **[Poulami Mukherjee](https://github.com/poulami-mukherjee)**

We would also like to extend our gratitude to the following contributors and supporters who have helped make MailyDaily better:

- Women Who Code Volunteers: For helping grow the project.
- The open-source community: For continuous feedback, suggestions, and code contributions.
- [Hugging Face](https://huggingface.co/): For providing the AI models that power email summarization and action items.

Special thanks to everyone who has supported or contributed to the development of MailyDaily!

---

Happy coding!
