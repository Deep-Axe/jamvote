# JamVote

JamVote is an Android-based real-time collaborative music streaming app, designed to enhance shared listening experiences in both physical and remote social environments.

Unlike existing collaborative playlist systems that rely on manual queue reordering or single-user dominance, JamVote introduces a non-blocking democratic governance model. This system dynamically manages song sequencing based on engagement, contextual relevance, and fairness constraints.

## Key Features

*   **Real-Time Collaborative Queue System:** Integrates real-time queue scoring and engagement-weighted feedback mechanisms to automatically reorder tracks.
*   **Non-Blocking Voting Mechanism:** Ensures seamless playback without requiring unanimous participation. Playback continues automatically even if no votes are cast.
*   **Gamification & Reputation System:** Incentivizes high-quality music curation and sustained user engagement through session-based awards and reputation scores.
*   **Content Filtering (SFW Mode):** Room-level toggle to block explicit tracks and keep the queue appropriate for the environment.

## Planned Features

*   **Vibe Continuity Engine:** Automatically suggests or inserts songs based on genre or metadata when the queue is low.

## Tech Stack

*   **Frontend:** Native Android (Java & XML) built in Android Studio.
*   **Build System:** Gradle (Groovy DSL).
*   **Backend & State Management:** Firebase Authentication for user login and Firebase Firestore/Realtime Database for room state and queue sync.
*   **Audio & Metadata:** YouTube IFrame Player API / YouTube Data API v3 for track metadata and playback.

## Getting Started

### Prerequisites

*   [Android Studio](https://developer.android.com/studio)
*   A Firebase Project
*   A YouTube Data API v3 Key

### Installation

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/Deep-Axe/jamvote.git
    ```
2.  **Open the project** in Android Studio.
3.  **Allow Gradle to sync** and download the required dependencies.
4.  **Connect Firebase:** Add your `google-services.json` file to the `app/` directory.

### Getting a YouTube API Key

1.  Go to the [Google Cloud Console](https://console.cloud.google.com/).
2.  Create a new project or select an existing one.
3.  Navigate to **APIs & Services > Library**.
4.  Search for "YouTube Data API v3" and enable it for your project.
5.  Go to **APIs & Services > Credentials**.
6.  Click **Create Credentials** and select **API Key**.
7.  Copy the generated API Key.
8.  *Important:* For security, edit the API Key settings to restrict its usage to "Android apps" and provide your app's package name and SHA-1 certificate fingerprint.
9.  **Add the key to your project:** Open `local.properties` in the root of your project and add the following line:
    ```properties
    youtube_api_key=YOUR_API_KEY_HERE
    ```

### Running the App

Build and run the app on an emulator or physical Android device from within Android Studio.
