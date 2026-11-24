# Claude Code handoff
Implement Kotlin Compose Material 3, MVVM, Room, DataStore, Media3, Navigation Compose. Use foreground service for user-initiated microphone sessions with visible notification and microphone service type; request RECORD_AUDIO before starting. Use local event classification with uncertainty; clips opt-in, delete/export locally. Alarm scheduling must handle exact alarm access and notification permissions. Do not promise alarms when device restrictions prevent delivery. Auth and billing are optional integrations, not implemented; use Play Billing localized prices, never hardcode. Audio assets must be original or licensed; no audio files included. Example metrics are mock data.

## Splash screen
Reference: screens/01_splash_screen.png
Purpose: Rest better. Wake refreshed.
Layout: hero template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Rest better. Wake refreshed.
Components / labels: Illustration or visualization; Continue.
Primary interaction: Continue → Welcome onboarding. Back returns to previous screen.
Assets: assets/icons/icon_back.svg, assets/images/midnight.png
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Welcome onboarding
Reference: screens/02_welcome_onboarding.png
Purpose: Make tonight a little calmer.
Layout: hero template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Make tonight a little calmer.
Components / labels: Illustration or visualization; Next.
Primary interaction: Next → Sound library onboarding. Back returns to previous screen.
Assets: assets/icons/icon_back.svg, assets/images/midnight.png
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Sound library onboarding
Reference: screens/03_sound_library_onboarding.png
Purpose: Find your perfect soundscape. All sounds work offline.
Layout: hero template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Find your perfect soundscape. All sounds work offline.
Components / labels: Illustration or visualization; Next.
Primary interaction: Next → Sleep analysis onboarding. Back returns to previous screen.
Assets: assets/icons/icon_back.svg, assets/images/rain.png
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Sleep analysis onboarding
Reference: screens/04_sleep_analysis_onboarding.png
Purpose: Understand overnight sound activity with your microphone.
Layout: hero template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Understand overnight sound activity with your microphone.
Components / labels: Illustration or visualization; Next.
Primary interaction: Next → Privacy onboarding. Back returns to previous screen.
Assets: assets/icons/icon_back.svg, assets/images/noise.png
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Privacy onboarding
Reference: screens/05_privacy_onboarding.png
Purpose: Your sleep data stays on this device by default.
Layout: hero template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Your sleep data stays on this device by default.
Components / labels: Illustration or visualization; Continue.
Primary interaction: Continue → Microphone permission. Back returns to previous screen.
Assets: assets/icons/icon_back.svg, assets/images/midnight.png
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Microphone permission
Reference: screens/06_microphone_permission.png
Purpose: Allow microphone access only when you start a sleep session.
Layout: state template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Allow microphone access only when you start a sleep session.
Components / labels: Not now; Allow microphone.
Primary interaction: Allow microphone → Notification permission. Back returns to previous screen.
Assets: assets/icons/icon_back.svg, assets/illustrations/empty.png
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Notification permission
Reference: screens/07_notification_permission.png
Purpose: Get bedtime reminders and alarm notifications.
Layout: state template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Get bedtime reminders and alarm notifications.
Components / labels: Not now; Continue.
Primary interaction: Continue → Personal sleep goal. Back returns to previous screen.
Assets: assets/icons/icon_back.svg, assets/illustrations/empty.png
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Personal sleep goal
Reference: screens/08_personal_sleep_goal.png
Purpose: Choose a bedtime routine that works for you.
Layout: list template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Choose a bedtime routine that works for you.
Components / labels: Sleep target • 8 hours; Bedtime • 10:30 PM; Wake time • 6:30 AM; Finish setup.
Primary interaction: Finish setup → Home dashboard. Back returns to previous screen.
Assets: assets/icons/icon_back.svg
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Login
Reference: screens/09_login.png
Purpose: Sign in to your optional account.
Layout: form template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Sign in to your optional account.
Components / labels: Email address; Password; Forgot password; Create account; Continue as guest; Sign in.
Primary interaction: Sign in → Home dashboard. Back returns to previous screen.
Assets: assets/icons/icon_back.svg
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Sign up
Reference: screens/10_sign_up.png
Purpose: Create an optional account.
Layout: form template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Create an optional account.
Components / labels: Name; Email address; Password; Accept terms and privacy policy; Create account.
Primary interaction: Create account → Home dashboard. Back returns to previous screen.
Assets: assets/icons/icon_back.svg
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Forgot password
Reference: screens/11_forgot_password.png
Purpose: Enter your account email to request a reset.
Layout: form template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Enter your account email to request a reset.
Components / labels: Email address; Send code.
Primary interaction: Send code → Reset verification. Back returns to previous screen.
Assets: assets/icons/icon_back.svg
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Reset verification
Reference: screens/12_reset_verification.png
Purpose: Enter the code sent to your email.
Layout: form template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Enter the code sent to your email.
Components / labels: Verification code; Resend code; Verify code.
Primary interaction: Verify code → New password. Back returns to previous screen.
Assets: assets/icons/icon_back.svg
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## New password
Reference: screens/13_new_password.png
Purpose: Choose a new secure password.
Layout: form template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Choose a new secure password.
Components / labels: New password; Confirm password; Reset password.
Primary interaction: Reset password → Password reset success. Back returns to previous screen.
Assets: assets/icons/icon_back.svg
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Password reset success
Reference: screens/14_password_reset_success.png
Purpose: Your password has been updated.
Layout: state template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Your password has been updated.
Components / labels: Illustration or visualization; Back to login.
Primary interaction: Back to login → Login. Back returns to previous screen.
Assets: assets/icons/icon_back.svg, assets/illustrations/success.png
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Home dashboard
Reference: screens/15_home_dashboard.png
Purpose: Good evening. Make time for rest.
Layout: home template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Good evening. Make time for rest.
Components / labels: Gentle rain; Ocean waves; Brown noise; Start sleep tracking.
Primary interaction: Start sleep tracking → Sleep session setup. Back returns to previous screen.
Assets: assets/icons/icon_back.svg, assets/icons/icon_home.svg, assets/icons/icon_insights.svg, assets/icons/icon_profile.svg, assets/icons/icon_sleep.svg, assets/icons/icon_sounds.svg, assets/images/midnight.png
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Search
Reference: screens/16_search.png
Purpose: Find your favorite sounds.
Layout: list template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Find your favorite sounds.
Components / labels: Search sounds; Recent • Rain; Popular • Ocean waves; Popular • White noise; Search.
Primary interaction: Search → Search results. Back returns to previous screen.
Assets: assets/icons/icon_back.svg
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Search results
Reference: screens/17_search_results.png
Purpose: Results for rain
Layout: sounds template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Results for rain
Components / labels: Gentle rain; Rain on window; Forest rain; Play gentle rain.
Primary interaction: Play gentle rain → Full audio player. Back returns to previous screen.
Assets: assets/icons/icon_back.svg, assets/icons/icon_play.svg, assets/icons/icon_search.svg, assets/images/forest.png, assets/images/ocean.png, assets/images/rain.png
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Notifications
Reference: screens/18_notifications.png
Purpose: Your bedtime updates.
Layout: list template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Your bedtime updates.
Components / labels: Bedtime reminder • 10:30 PM; Sleep report ready • Yesterday; Manage reminders.
Primary interaction: Manage reminders → Reminder settings. Back returns to previous screen.
Assets: assets/icons/icon_back.svg
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Sound recommendations
Reference: screens/19_sound_recommendations.png
Purpose: A quieter evening starts here.
Layout: sounds template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: A quieter evening starts here.
Components / labels: Gentle rain; Ocean waves; Forest ambience; Explore library.
Primary interaction: Explore library → Sound library. Back returns to previous screen.
Assets: assets/icons/icon_back.svg, assets/icons/icon_play.svg, assets/icons/icon_search.svg, assets/images/forest.png, assets/images/ocean.png, assets/images/rain.png
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Sound library
Reference: screens/20_sound_library.png
Purpose: Discover your perfect atmosphere.
Layout: sounds template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Discover your perfect atmosphere.
Components / labels: Gentle rain; Ocean waves; Forest ambience; Fireplace; White noise; Brown noise; Open mixer.
Primary interaction: Open mixer → Sound mixer. Back returns to previous screen.
Assets: assets/icons/icon_back.svg, assets/icons/icon_home.svg, assets/icons/icon_insights.svg, assets/icons/icon_play.svg, assets/icons/icon_profile.svg, assets/icons/icon_search.svg, assets/icons/icon_sleep.svg, assets/icons/icon_sounds.svg, assets/images/fireplace.png, assets/images/forest.png, assets/images/midnight.png, assets/images/noise.png, assets/images/ocean.png, assets/images/rain.png
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Category details
Reference: screens/21_category_details.png
Purpose: Nature sounds • Available offline
Layout: sounds template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Nature sounds • Available offline
Components / labels: Gentle rain; Ocean waves; Forest ambience; Play gentle rain.
Primary interaction: Play gentle rain → Full audio player. Back returns to previous screen.
Assets: assets/icons/icon_back.svg, assets/icons/icon_play.svg, assets/icons/icon_search.svg, assets/images/forest.png, assets/images/ocean.png, assets/images/rain.png
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Sound details
Reference: screens/22_sound_details.png
Purpose: Gentle rain • A soft, steady rainfall.
Layout: player template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Gentle rain • A soft, steady rainfall.
Components / labels: Illustration or visualization; Play sound.
Primary interaction: Play sound → Full audio player. Back returns to previous screen.
Assets: assets/icons/icon_back.svg, assets/icons/icon_heart.svg, assets/icons/icon_play.svg, assets/icons/icon_timer.svg, assets/images/rain.png
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Full audio player
Reference: screens/23_full_audio_player.png
Purpose: Gentle rain • Nature collection
Layout: player template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Gentle rain • Nature collection
Components / labels: Illustration or visualization; Set sleep timer.
Primary interaction: Set sleep timer → Sleep timer. Back returns to previous screen.
Assets: assets/icons/icon_back.svg, assets/icons/icon_heart.svg, assets/icons/icon_play.svg, assets/icons/icon_timer.svg, assets/images/rain.png
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Sound mixer
Reference: screens/24_sound_mixer.png
Purpose: Layer sounds to create your own atmosphere.
Layout: mix template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Layer sounds to create your own atmosphere.
Components / labels: Gentle rain; Ocean waves; Brown noise; Add sound; Save mix.
Primary interaction: Save mix → Save custom mix. Back returns to previous screen.
Assets: assets/icons/icon_back.svg
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Add sound to mixer
Reference: screens/25_add_sound_to_mixer.png
Purpose: Choose a sound to add to your mix.
Layout: sounds template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Choose a sound to add to your mix.
Components / labels: Gentle rain; Ocean waves; Forest ambience; Fireplace; Add selected sound.
Primary interaction: Add selected sound → Sound mixer. Back returns to previous screen.
Assets: assets/icons/icon_back.svg, assets/icons/icon_play.svg, assets/icons/icon_search.svg, assets/images/fireplace.png, assets/images/forest.png, assets/images/ocean.png, assets/images/rain.png
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Save custom mix
Reference: screens/26_save_custom_mix.png
Purpose: Give your soundscape a name.
Layout: form template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Give your soundscape a name.
Components / labels: Mix name; Save mix.
Primary interaction: Save mix → My mixes. Back returns to previous screen.
Assets: assets/icons/icon_back.svg
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## My mixes
Reference: screens/27_my_mixes.png
Purpose: Your personal soundscapes.
Layout: sounds template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Your personal soundscapes.
Components / labels: Midnight calm; Ocean escape; Create mix.
Primary interaction: Create mix → Sound mixer. Back returns to previous screen.
Assets: assets/icons/icon_back.svg, assets/icons/icon_play.svg, assets/icons/icon_search.svg, assets/images/ocean.png, assets/images/rain.png
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Favorites
Reference: screens/28_favorites.png
Purpose: Your favorite sounds, ready to play.
Layout: sounds template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Your favorite sounds, ready to play.
Components / labels: Gentle rain; Ocean waves; Explore sounds.
Primary interaction: Explore sounds → Sound library. Back returns to previous screen.
Assets: assets/icons/icon_back.svg, assets/icons/icon_play.svg, assets/icons/icon_search.svg, assets/images/ocean.png, assets/images/rain.png
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Sleep timer
Reference: screens/29_sleep_timer.png
Purpose: Let your audio fade out gently.
Layout: list template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Let your audio fade out gently.
Components / labels: 15 minutes; 30 minutes • Selected; 60 minutes; Custom duration; Fade out • 5 minutes; Start timer.
Primary interaction: Start timer → Full audio player. Back returns to previous screen.
Assets: assets/icons/icon_back.svg
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Filter and sorting
Reference: screens/30_filter_and_sorting.png
Purpose: Customize your sound library.
Layout: list template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Customize your sound library.
Components / labels: All categories; Nature; Noise; Sort • Most popular; Offline only; Apply filters.
Primary interaction: Apply filters → Sound library. Back returns to previous screen.
Assets: assets/icons/icon_back.svg
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Mini-player state
Reference: screens/31_mini_player_state.png
Purpose: Keep exploring while listening.
Layout: home template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Keep exploring while listening.
Components / labels: Gentle rain; Ocean waves; Open player.
Primary interaction: Open player → Full audio player. Back returns to previous screen.
Assets: assets/icons/icon_back.svg, assets/icons/icon_home.svg, assets/icons/icon_insights.svg, assets/icons/icon_profile.svg, assets/icons/icon_sleep.svg, assets/icons/icon_sounds.svg, assets/images/midnight.png
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Sleep dashboard
Reference: screens/32_sleep_dashboard.png
Purpose: Build a more consistent sleep routine.
Layout: sleep template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Build a more consistent sleep routine.
Components / labels: Last session • 7h 42m; Wake alarm • 6:30 AM; Start sleep tracking.
Primary interaction: Start sleep tracking → Sleep session setup. Back returns to previous screen.
Assets: assets/icons/icon_back.svg, assets/icons/icon_home.svg, assets/icons/icon_insights.svg, assets/icons/icon_profile.svg, assets/icons/icon_sleep.svg, assets/icons/icon_sounds.svg, assets/images/midnight.png
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Sleep session setup
Reference: screens/33_sleep_session_setup.png
Purpose: Prepare for a quiet night.
Layout: list template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Prepare for a quiet night.
Components / labels: Wake alarm • 6:30 AM; Sound monitoring • On; Relaxation audio • Gentle rain; Save event clips • Off; Continue.
Primary interaction: Continue → Microphone test. Back returns to previous screen.
Assets: assets/icons/icon_back.svg
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Wake-up alarm
Reference: screens/34_wake_up_alarm.png
Purpose: Choose when to wake up.
Layout: list template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Choose when to wake up.
Components / labels: 06:30 AM; Repeat • Weekdays; Alarm sound • Soft chime; Vibration • On; Save alarm.
Primary interaction: Save alarm → Sleep session setup. Back returns to previous screen.
Assets: assets/icons/icon_back.svg
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Microphone test
Reference: screens/35_microphone_test.png
Purpose: Check that your microphone can hear nearby sounds.
Layout: track template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Check that your microphone can hear nearby sounds.
Components / labels: Illustration or visualization; Continue.
Primary interaction: Continue → Phone placement guide. Back returns to previous screen.
Assets: assets/icons/icon_back.svg
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Phone placement guide
Reference: screens/36_phone_placement_guide.png
Purpose: Place your phone on a stable bedside surface, uncovered.
Layout: hero template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Place your phone on a stable bedside surface, uncovered.
Components / labels: Illustration or visualization; Start session.
Primary interaction: Start session → Active sleep tracking. Back returns to previous screen.
Assets: assets/icons/icon_back.svg, assets/images/midnight.png
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Active sleep tracking
Reference: screens/37_active_sleep_tracking.png
Purpose: Monitoring sound activity on this device.
Layout: track template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Monitoring sound activity on this device.
Components / labels: Pause tracking; End session.
Primary interaction: End session → End session confirmation. Back returns to previous screen.
Assets: assets/icons/icon_back.svg
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Tracking paused
Reference: screens/38_tracking_paused.png
Purpose: Sound monitoring is paused.
Layout: track template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Sound monitoring is paused.
Components / labels: Illustration or visualization; Resume tracking.
Primary interaction: Resume tracking → Active sleep tracking. Back returns to previous screen.
Assets: assets/icons/icon_back.svg
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## End session confirmation
Reference: screens/39_end_session_confirmation.png
Purpose: Finish and save this sleep session?
Layout: dialog template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Finish and save this sleep session?
Components / labels: Keep tracking; End and save.
Primary interaction: End and save → Analyzing session. Back returns to previous screen.
Assets: assets/icons/icon_back.svg, assets/illustrations/empty.png
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Analyzing session
Reference: screens/40_analyzing_session.png
Purpose: Preparing your overnight sound summary.
Layout: state template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Preparing your overnight sound summary.
Components / labels: Illustration or visualization; View when ready.
Primary interaction: View when ready → Morning sleep summary. Back returns to previous screen.
Assets: assets/icons/icon_back.svg, assets/illustrations/empty.png
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Morning sleep summary
Reference: screens/41_morning_sleep_summary.png
Purpose: Your night at a glance.
Layout: report template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Your night at a glance.
Components / labels: Session duration • 7h 42m; Sound events • 12; Quiet intervals • 5h 18m; View full report.
Primary interaction: View full report → Daily sleep report. Back returns to previous screen.
Assets: assets/icons/icon_back.svg
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Insights dashboard
Reference: screens/42_insights_dashboard.png
Purpose: Discover your sleep routine over time.
Layout: report template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Discover your sleep routine over time.
Components / labels: Average session • 7h 24m; Bedtime consistency • 5 of 7 nights; View history.
Primary interaction: View history → Sleep history. Back returns to previous screen.
Assets: assets/icons/icon_back.svg, assets/icons/icon_home.svg, assets/icons/icon_insights.svg, assets/icons/icon_profile.svg, assets/icons/icon_sleep.svg, assets/icons/icon_sounds.svg
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Daily sleep report
Reference: screens/43_daily_sleep_report.png
Purpose: Tuesday, 24 November • Example session
Layout: report template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Tuesday, 24 November • Example session
Components / labels: Session duration • 7h 42m; Quiet intervals • 5h 18m; Sound events • 12; View sound events.
Primary interaction: View sound events → Sound event timeline. Back returns to previous screen.
Assets: assets/icons/icon_back.svg
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Weekly report
Reference: screens/44_weekly_report.png
Purpose: 18–24 November • Example data
Layout: report template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: 18–24 November • Example data
Components / labels: Average session • 7h 24m; Sessions • 6; View monthly report.
Primary interaction: View monthly report → Monthly report. Back returns to previous screen.
Assets: assets/icons/icon_back.svg
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Monthly report
Reference: screens/45_monthly_report.png
Purpose: November • Example data
Layout: report template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: November • Example data
Components / labels: Average session • 7h 16m; Sessions • 21; View history.
Primary interaction: View history → Sleep history. Back returns to previous screen.
Assets: assets/icons/icon_back.svg
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Sleep history
Reference: screens/46_sleep_history.png
Purpose: Your previous sleep sessions.
Layout: list template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Your previous sleep sessions.
Components / labels: 24 Nov • 7h 42m; 23 Nov • 7h 12m; 22 Nov • 8h 05m; 21 Nov • 6h 50m; Open latest session.
Primary interaction: Open latest session → Daily sleep report. Back returns to previous screen.
Assets: assets/icons/icon_back.svg
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Sound event timeline
Reference: screens/47_sound_event_timeline.png
Purpose: Estimated sound events, not a diagnosis.
Layout: list template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Estimated sound events, not a diagnosis.
Components / labels: 01:12 • Possible snoring; 02:45 • Environmental noise; 04:18 • Possible snoring; Open event.
Primary interaction: Open event → Recorded event details. Back returns to previous screen.
Assets: assets/icons/icon_back.svg
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Recorded event details
Reference: screens/48_recorded_event_details.png
Purpose: Possible snoring • 01:12 AM
Layout: track template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Possible snoring • 01:12 AM
Components / labels: Audio clip not saved; Back to timeline.
Primary interaction: Back to timeline → Sound event timeline. Back returns to previous screen.
Assets: assets/icons/icon_back.svg
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Sleep goals
Reference: screens/49_sleep_goals.png
Purpose: Set a realistic nightly routine.
Layout: list template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Set a realistic nightly routine.
Components / labels: Target • 8 hours; Bedtime • 10:30 PM; Wake time • 6:30 AM; Save goals.
Primary interaction: Save goals → Insights dashboard. Back returns to previous screen.
Assets: assets/icons/icon_back.svg
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Profile
Reference: screens/50_profile.png
Purpose: Your sleep space.
Layout: list template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Your sleep space.
Components / labels: Guest profile; Edit profile; Sign in; My mixes; Favorites; Premium; Open settings.
Primary interaction: Open settings → Settings. Back returns to previous screen.
Assets: assets/icons/icon_back.svg, assets/icons/icon_home.svg, assets/icons/icon_insights.svg, assets/icons/icon_profile.svg, assets/icons/icon_sleep.svg, assets/icons/icon_sounds.svg
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Edit profile
Reference: screens/51_edit_profile.png
Purpose: Personalize your local profile.
Layout: form template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Personalize your local profile.
Components / labels: Display name; Save changes.
Primary interaction: Save changes → Profile. Back returns to previous screen.
Assets: assets/icons/icon_back.svg
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Settings
Reference: screens/52_settings.png
Purpose: Make the app work for you.
Layout: list template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Make the app work for you.
Components / labels: Audio settings; Sleep tracking settings; Reminder settings; Appearance; Privacy and data; Help and FAQ; Back to profile.
Primary interaction: Back to profile → Profile. Back returns to previous screen.
Assets: assets/icons/icon_back.svg
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Audio settings
Reference: screens/53_audio_settings.png
Purpose: Adjust your listening experience.
Layout: list template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Adjust your listening experience.
Components / labels: Default volume • 60%; Fade out • 5 minutes; Background playback • On; Save preferences.
Primary interaction: Save preferences → Settings. Back returns to previous screen.
Assets: assets/icons/icon_back.svg
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Sleep tracking settings
Reference: screens/54_sleep_tracking_settings.png
Purpose: Control overnight monitoring.
Layout: list template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Control overnight monitoring.
Components / labels: Microphone monitoring • On; Save event clips • Off; Retention • 30 days; Save preferences.
Primary interaction: Save preferences → Settings. Back returns to previous screen.
Assets: assets/icons/icon_back.svg
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Reminder settings
Reference: screens/55_reminder_settings.png
Purpose: Build your bedtime routine.
Layout: list template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Build your bedtime routine.
Components / labels: Bedtime reminder • On; Reminder time • 10:00 PM; Repeat • Daily; Save reminders.
Primary interaction: Save reminders → Settings. Back returns to previous screen.
Assets: assets/icons/icon_back.svg
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Appearance
Reference: screens/56_appearance.png
Purpose: Choose your preferred look.
Layout: list template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Choose your preferred look.
Components / labels: Midnight dark • Selected; System theme; Reduce motion • Off; Apply appearance.
Primary interaction: Apply appearance → Settings. Back returns to previous screen.
Assets: assets/icons/icon_back.svg
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Privacy and data
Reference: screens/57_privacy_and_data.png
Purpose: You control your local information.
Layout: list template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: You control your local information.
Components / labels: Microphone access; Export sleep history; Delete sleep data; Privacy policy; Manage deletion.
Primary interaction: Manage deletion → Delete data confirmation. Back returns to previous screen.
Assets: assets/icons/icon_back.svg
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Help and FAQ
Reference: screens/58_help_and_faq.png
Purpose: Find answers to common questions.
Layout: list template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Find answers to common questions.
Components / labels: How does monitoring work?; Why is microphone access needed?; Can I listen offline?; Contact support; Back to settings.
Primary interaction: Back to settings → Settings. Back returns to previous screen.
Assets: assets/icons/icon_back.svg
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Premium subscription
Reference: screens/59_premium_subscription.png
Purpose: More soundscapes. More flexibility.
Layout: list template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: More soundscapes. More flexibility.
Components / labels: Monthly plan • Store price; Annual plan • Store price; Lifetime • Store price; Restore purchases; View purchase options.
Primary interaction: View purchase options → Profile. Back returns to previous screen.
Assets: assets/icons/icon_back.svg
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Empty state
Reference: screens/60_empty_state.png
Purpose: No sleep sessions yet.
Layout: state template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: No sleep sessions yet.
Components / labels: Illustration or visualization; Start a session.
Primary interaction: Start a session → Sleep dashboard. Back returns to previous screen.
Assets: assets/icons/icon_back.svg, assets/illustrations/empty.png
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Loading state
Reference: screens/61_loading_state.png
Purpose: Preparing your sound library.
Layout: state template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Preparing your sound library.
Components / labels: Illustration or visualization; Cancel.
Primary interaction: Cancel → Home dashboard. Back returns to previous screen.
Assets: assets/icons/icon_back.svg, assets/illustrations/empty.png
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Error state
Reference: screens/62_error_state.png
Purpose: We could not load this content. Your data is safe.
Layout: state template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: We could not load this content. Your data is safe.
Components / labels: Illustration or visualization; Try again.
Primary interaction: Try again → Sound library. Back returns to previous screen.
Assets: assets/icons/icon_back.svg, assets/illustrations/error.png
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Permission denied
Reference: screens/63_permission_denied.png
Purpose: Microphone access is needed for sound monitoring.
Layout: state template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Microphone access is needed for sound monitoring.
Components / labels: Continue without tracking; Open Android settings.
Primary interaction: Open Android settings → Sleep session setup. Back returns to previous screen.
Assets: assets/icons/icon_back.svg, assets/illustrations/error.png
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Success state
Reference: screens/64_success_state.png
Purpose: Your sound mix has been saved.
Layout: state template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Your sound mix has been saved.
Components / labels: Illustration or visualization; View my mixes.
Primary interaction: View my mixes → My mixes. Back returns to previous screen.
Assets: assets/icons/icon_back.svg, assets/illustrations/success.png
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.


## Delete data confirmation
Reference: screens/65_delete_data_confirmation.png
Purpose: Permanently delete local sleep history and saved clips?
Layout: dialog template, 20dp gutters, 54dp primary CTA, 48dp minimum hit areas.
Content: Permanently delete local sleep history and saved clips?
Components / labels: Cancel; Delete data.
Primary interaction: Delete data → Privacy and data. Back returns to previous screen.
Assets: assets/icons/icon_back.svg, assets/illustrations/empty.png
States: loading, empty, error where data-dependent; disabled while submitting; permission denial where applicable.
