# BuildTrack v1 — Plain-English Guide

## 1. What the app is

BuildTrack is an Android app for a construction site. Three kinds of people log in:

- **Manager** — creates a project site, assigns an engineer and a buyer, writes short instructions, and watches progress.
- **On-site Engineer** — opens the app every day, fills a daily log of materials used, labour on site, work done, weather, and any site photos, then submits it.
- **Buyer / Owner** — sees a read-only dashboard of the same project: progress percentage, last report date, manager contact, site photos, and the manager’s note.

Everything is stored in the cloud using **Firebase**. The phone does not keep a local database; it reads from and writes to Firebase Firestore over the internet.

---

## 2. The big pieces

The source code is split into three main places:

1. **Java files** (`app/src/main/java/com/example/buildtrack_v1/`): the brains. They decide what screen to show, fetch data, react to button taps, and save things to Firebase.
2. **XML layout files** (`app/src/main/res/layout/`): the blueprints. They describe where buttons, text, progress bars, and cards appear on each screen.
3. **Drawable files** (`app/src/main/res/drawable/`): small shape or icon files used for backgrounds, borders, and buttons.

A fourth piece lives outside the phone: the **Firebase project** named `buildtrack-89f4f`. It stores the actual data in collections like a set of folders.

---

## 3. Firebase data model

Think of Firestore as a filing cabinet. Each drawer is a collection, each file is a document, and each document is a set of fields.

### `users`
One document per registered account. Fields include name, email, role (manager, engineer, buyer), phone number, and an engineer code like `ENG-8156`.

### `projects`
One document per construction site. It stores the project name, location, manager’s user id, assigned engineers and buyers, start and end dates, the current stage progress, and a short manager note.

### `dailyLogs`
One document per daily log submitted by an engineer. It links to a project, stores materials, labour count, work description, weather, remarks, a timestamp, and references to any photos.

### `sitePhotos`
A new collection where each document holds one photo encoded as a base64 string. Because Firebase Storage is not configured for this project, photos are stored as Firestore documents instead. A daily log stores a reference like `fs:abc123`, which points to a `sitePhotos` document.

### `messages`
Documents for the in-app chat between a manager and the engineers assigned to a project.

---

## 4. Java files and what each one does

### Activities — full screens

An Activity in Android is one screen. When you tap a button, the app often starts a new Activity.

**MainActivity.java** — the login screen. It reads the email, password, and selected role radio button. It signs the user in through Firebase Authentication and then opens the correct home screen: ManagerHomeActivity, EngineerHomeActivity, or BuyerHomeActivity.

**RegisterActivity.java** — the sign-up screen. It collects first name, last name, email, mobile number, password, and role. It creates a Firebase Auth account and writes a profile document into the `users` collection.

**ManagerHomeActivity.java** — the manager’s landing page. It shows a list of only the projects that manager created. It uses DailyLogRepository.observeManagerProjects to keep the list live. From here the manager can create a new project or open an existing one.

**EngineerHomeActivity.java** — the engineer’s landing page. It shows only the projects assigned to that engineer. The engineer can tap a card to open the daily log form.

**BuyerHomeActivity.java** — the buyer’s read-only dashboard. It shows the assigned project, overall progress, number of site logs, last report date, and the manager’s contact card with Call and Message buttons.

**ProjectDetailActivity.java** — the manager’s control centre for one project. It shows options like Daily Logs, Edit Progress, Assign Team, Site Comments, and Post Photo. It is the hub from which the manager picks an action.

**DailyLogEntryActivity.java** — the form an engineer fills every day. It has sliders for each construction stage, fields for materials and labour, a weather dropdown, remarks, a camera button for photos, and a Submit button. When submitted, it creates a new `dailyLogs` document and updates the project’s stage progress.

**PhaseProgressActivity.java** — the screen where the engineer or manager adjusts stage sliders. The overall progress shown everywhere else is simply the average of these stage values.

**ManagerLogsActivity.java** — a detailed site report used by managers and buyers. It shows helpline, overall work and schedule progress bars, and every engineer’s log stacked below. Managers can edit the helpline; buyers see it read-only.

**ProgressActivity.java** — the buyer’s dedicated progress view. It displays the overall stage breakdown and the latest log summary.

**UpdatesFeedActivity.java** — a scrollable feed of all daily logs for a project, newest first. It is read-only.

**LogHistoryActivity.java** — another log list, used from the engineer side to see previously submitted logs.

**AssignPeopleActivity.java** — where the manager types the email of an engineer and a buyer to assign them to a project. The app looks up the user document and stores their user ids in the project document.

**CommentsActivity.java** — where the manager writes a short instruction note that appears on the engineer’s project card.

**PostUpdateActivity.java** — lets a manager post a quick photo update with a caption. It uses the phone’s image picker.

**ChatActivity.java** — live text chat for a project between the manager and assigned engineers. It listens to the `messages` collection and scrolls new messages into view.

---

### Repository and helpers

**DailyLogRepository.java** — the single data layer. Every other file asks this class for data instead of talking to Firestore directly. It has methods like:

- observeManagerProjects — live list filtered by manager id.
- observeEngineerProjects — live list filtered by assigned engineer.
- observeBuyerProjects — live list filtered by assigned buyer.
- observeLogsForProject — live list of daily logs for one project.
- saveLogWithPhotos — saves a daily log and stores each attached photo as a `sitePhotos` document.
- createProject, updateProject, assignPeople, savePhone, sendMessage, and many more.

It uses Callback objects because Firebase calls are asynchronous: the phone asks for data, Firebase answers later, and the Callback says what to do with the answer.

**ImageHelper.java** — loads photos into image views. If the stored value starts with `fs:`, it fetches the matching `sitePhotos` document, caches the base64 string, and displays it. If it is an older web URL, it loads that directly using the Glide image library.

**ProgressUtil.java** — small shared helpers for progress and status text. For example, it decides whether a project is “On track”, “Behind schedule”, or “Completed” by comparing work progress to schedule progress. It also draws the status as a simple dot instead of a filled pill.

**MobileNumber.java** — normalizes phone numbers. It strips spaces and country codes, checks that it is a valid 10-digit Indian mobile number, formats it as `+91 98200 11223`, and opens the dialer or SMS app when Call or Message is tapped.

**ContactNumberSetup.java** — asks a user once for their mobile number if it is missing from their profile. This is important because older accounts were created before phone numbers were required.

**EngineerIdentity.java** — generates a readable engineer code like `ENG-8156` from a Firebase user id. The same user always gets the same code.

**UiTheme.java** — reads the current theme colours so Java code can colour views the same way XML does. It also picks the manager or engineer theme when both roles share a screen.

---

### Adapters — turning lists into cards

When a screen shows a scrollable list, an adapter creates each row.

**ProjectAdapter.java** — used by EngineerHomeActivity. It binds one project to one engineer card layout, setting the project name, assigned-by text, location, due date, status dot, and manager note.

**ManagerProjectAdapter.java** — used by ManagerHomeActivity. It binds one project to the manager card layout, which includes work progress, schedule progress, assigned engineer/buyer counts, last update, and the overflow menu.

**LogHistoryAdapter.java** — used by LogHistoryActivity. It creates rows for submitted daily logs.

**ChatAdapter.java** — used by ChatActivity. It creates incoming and outgoing message bubbles, coloured by sender.

---

### Data classes — plain containers

**UserInfo.java** — holds a user profile: name, email, role, engineer id, and phone. It has getters and setters so Firestore can convert documents into Java objects automatically.

**DailyLog.java** — holds one daily log. It includes nested classes MaterialEntry and LabourEntry so a single log can carry lists of materials and labour trades.

---

## 5. XML layouts and what each one describes

XML layouts are like recipes: they say which widgets go where, but not what happens when you tap them.

### Activity screens

**activity_main.xml** — login screen. Contains the BuildTrack header, email and password inputs, a show-password checkbox, role radio buttons, the Login button, and the Create an account link.

**activity_register.xml** — sign-up screen. Contains inputs for first name, last name, email, mobile number, and password, plus role radio buttons, Register button, and a link back to login.

**activity_manager_home.xml** — manager home. Has a top bar with role title and menu button, a Projects section title, a New project button, a RecyclerView for the project list, and an empty-state message.

**activity_engineer_home.xml** — engineer home. Similar top bar, an ID badge, a project list RecyclerView, and an empty message.

**activity_buyer_home.xml** — buyer dashboard. Shows the project card with progress, stats, site photos button, and the manager contact card.

**project_detail.xml** — manager project hub. Displays project title, status, progress, and action buttons for Daily Logs, Edit Progress, Assign Team, Site Comments, and Post Photo.

**activity_daily_log.xml** — engineer daily log form. Holds project title, stage sliders, dynamic material and labour rows, weather/remarks fields, photo preview, and Submit.

**activity_manager_progress.xml** — manager/buyer detailed report. Contains summary progress bars, helpline controls, and a container for log entries.

**activity_progress.xml** — buyer progress view. Shows overall percentage, stage rows, and a no-progress state.

**activity_phase_progress.xml** — stage slider editor. Has a list of SeekBar rows for each construction stage and a Save button.

**activity_updates_feed.xml** — feed of all logs for a project.

**activity_log_history.xml** — history list of submitted engineer logs.

**activity_assign_people.xml** — assign team screen with email inputs for engineer and buyer and a unit number field.

**activity_comments.xml** — manager note screen showing the current active note and a field to write a new one.

**activity_post_update.xml** — manager quick photo post screen.

**activity_chat.xml** — chat screen with a RecyclerView for messages and an input field with send button.

**activity_item_log.xml** — layout for displaying a single daily log entry inside the detailed report.

### Reusable item and card layouts

**activity_project_card.xml** — the card shown in the engineer home list.

**activity_manager_project_card.xml** — the card shown in the manager home list.

**item_buyer_update.xml** — a row in the buyer updates feed.

**item_progress_row.xml** — one stage row used in progress views.

**item_phase_row.xml** — one stage slider row used in PhaseProgressActivity.

**item_log_photo.xml** — a small thumbnail shown for each photo attached to a log.

**item_team_member.xml** — a row used when listing assigned people.

**item_chat_message.xml** — a single chat bubble.

**item_manager_log.xml** — a log summary row used in manager views.

**activity_itemmaterial_row.xml** and **activity_itemlabour_row.xml** — rows for one material entry and one labour entry in the daily log form.

---

## 6. Drawable files

These are small shape definitions used as backgrounds.

- **bg_header.xml** — a rounded grey rectangle used as a header band.
- **bg_input.xml** — the rounded box behind text fields.
- **bg_login_card.xml** — the bordered rounded panel behind the login and register forms.
- **bg_popup.xml** — the background used for popup menus.
- **bg_chip.xml** — a small rounded background for badge-like labels.
- **ic_more_vert.xml** — the three vertical dots icon used for overflow menus.
- **ic_launcher_background.xml** and **ic_launcher_foreground.xml** — the app icon layers.

---

## 7. A real data flow: engineer logs work, buyer sees it

This example shows how the parts work together.

1. The engineer opens EngineerHomeActivity. DailyLogRepository.observeEngineerProjects listens to the projects collection and returns only projects where the engineer’s uid appears in assignedEngineers.

2. The engineer taps a project card. ProjectAdapter opens DailyLogEntryActivity and passes the project id.

3. The engineer moves stage sliders, adds materials and labour, takes a photo, and taps Submit.

4. DailyLogEntryActivity asks DailyLogRepository.saveLogWithPhotos to save the log. The repository scales the photo, converts it to base64, creates a `sitePhotos` document, and stores the reference as `fs:docId` inside the daily log.

5. The repository also updates the project document with the new average stage progress.

6. The buyer opens BuyerHomeActivity. DailyLogRepository.observeBuyerProjects returns the assigned project. The buyer card reads the latest log count, last report date, and overall progress from the project document.

7. If the buyer taps View progress, ProgressActivity fetches the project stages and the latest logs to draw the detailed breakdown.

8. If the buyer taps Site Photos, the app reads the photo references from the logs and ImageHelper loads each one by fetching its `sitePhotos` document.

---

## 8. Key Android concepts used everywhere

**Activity** — one full screen of the app, like a page in a website.

**Intent** — a request to open another Activity. Intents can carry extras, small pieces of data like a project id or project name.

**findViewById** — the way Java grabs a view defined in XML so it can change text, colour, or visibility.

**RecyclerView** — a scrollable list that reuses row views for memory efficiency.

**Adapter** — the class that creates each row in a RecyclerView and fills it with data.

**Callback** — a small instruction passed to Firebase that says what to do when data arrives or an error happens.

**ListenerRegistration** — a live Firebase listener. It keeps the UI updated automatically whenever data changes. The app removes listeners when screens close to save battery.

**Theme attributes** — names like `?attr/colorPrimary` or `?attr/colorOnSurface`. They let the same layout look correct under different colour themes without hard-coding colours.

**Base64 encoding** — a way to turn a binary file like a photo into a long text string so it can be stored in Firestore.

---

## 9. How the app looks visually

The app uses a dark slate theme for managers and engineers with one blue accent colour. The buyer portal uses a calm light theme. Cards are flat with thin borders and rounded corners. Status is shown as a small coloured dot next to a label. Each project card has one primary action button and the rest of the actions hide behind a three-dot menu. The login and register screens are simple: brand name, subtitle, bordered card, plain radio rows separated by hairlines, one filled button, and a text link.

---

## 10. What you can test

- Create a manager account, log in, and create a project.
- Assign an engineer and a buyer to the project.
- Log in as the engineer, submit a daily log with materials, labour, and a photo.
- Log in as the buyer and confirm progress, last report date, site photos, and manager contact appear.
- Log in as the manager and confirm only your own projects appear, then open the detailed report.
