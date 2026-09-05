# Website/backend integration

The mobile app works with the existing API immediately, but exact cross-device resume, device heartbeat, activity, references, and browser context need the resource module in this directory.

This module is based on Vibe2Ship commit `43ca62c`. That version already has
task-linked sessions, references, and Focus preferences, so this integration
does not replace or duplicate those routes.

1. Copy `weft_sync.py` beside `Dashboard/backend/main.py`.
2. In `main.py`, after `app = FastAPI(...)` and the existing imports, add:

   ```python
   from weft_sync import install_mobile_oauth, install_weft_sync
   install_mobile_oauth(auth)
   install_weft_sync(app, db, get_current_user, privacy)
   ```

3. Add these names to `_USER_COLLECTIONS` in both `db.py` and `db_mock.py` so privacy export/delete includes them:

   ```python
   "work_states", "activities", "devices"
   ```

4. Deploy the backend. No website UI change is required: its existing task/session polling already reflects phone actions. To show the richer Resume card on the website, fetch `/api/work-state` alongside `/api/sessions`.

5. Set the backend environment variable `GOOGLE_ANDROID_CLIENT_ID` to the
   Android OAuth client ID. The installer verifies Google signatures and then
   accepts only the website or Android audience.

6. Update the browser extension to call `PUT /api/browser-context` with only `{title, url, relevant, captured_at, device_id}` while work state is ACTIVE/PAUSED. Continue using the website's existing `/api/references` route only after the user chooses Save Reference. Do not send page content or background history.

The supplied module deliberately strips `user_id` and FCM tokens from responses, scopes all records by the verified account, sanitizes URLs using the website's privacy module, and limits text lengths. FCM uses the backend's existing Firebase Application Default Credentials; no Firebase Admin key is included in the app or repository.
