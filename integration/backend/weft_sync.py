"""Cross-device state resources for WEFT.

Copy beside Dashboard/backend/main.py and call ``install_weft_sync`` after the
FastAPI app is created. This intentionally stores metadata and explicit user
notes only; it never accepts page HTML, keystrokes, history, messages, or page
content.
"""
from __future__ import annotations

from datetime import datetime, timezone
import os
from urllib.parse import parse_qsl, urlencode, urlsplit, urlunsplit
from typing import Any, Callable, Literal, Optional

from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel, Field, field_validator


def _now() -> str:
    return datetime.now(timezone.utc).isoformat()


_SENSITIVE_QUERY_MARKERS = ("token", "password", "secret", "auth", "session", "code", "key")


def _safe_url(value: str, privacy: Any) -> str:
    """Validate a URL and remove credentials, fragments, and secret-like query fields."""
    checked = privacy.clean_url(value)
    parts = urlsplit(checked)
    host = parts.hostname or ""
    if parts.port:
        host = f"{host}:{parts.port}"
    query = urlencode([
        (key, item) for key, item in parse_qsl(parts.query, keep_blank_values=True)
        if not any(marker in key.lower() for marker in _SENSITIVE_QUERY_MARKERS)
    ])
    return urlunsplit((parts.scheme, host, parts.path, query, ""))


class BrowserContextIn(BaseModel):
    title: str = Field(max_length=300)
    url: str = Field(max_length=2048)
    relevant: Optional[bool] = None
    captured_at: Optional[str] = None
    device_id: Optional[str] = Field(default=None, max_length=128)


class WorkStateIn(BaseModel):
    task_id: Optional[int] = None
    workflow_id: Optional[int] = None
    current_step_index: int = Field(default=0, ge=0)
    current_step: str = Field(default="", max_length=500)
    last_activity: str = Field(default="", max_length=1000)
    next_action: str = Field(default="", max_length=1000)
    status: Literal["IDLE", "ACTIVE", "PAUSED"] = "IDLE"
    active_session_id: Optional[int] = None
    started_at: Optional[str] = None
    device_id: Optional[str] = Field(default=None, max_length=128)
    browser_context: Optional[BrowserContextIn] = None


class DeviceIn(BaseModel):
    device_id: str = Field(min_length=1, max_length=128)
    name: str = Field(default="Unknown device", max_length=200)
    device_type: str = Field(default="unknown", max_length=40)
    capabilities: list[str] = Field(default_factory=list, max_length=30)
    push_token: Optional[str] = Field(default=None, max_length=500)


class Store:
    """Small adapter over the website's Firestore or in-memory db module."""
    def __init__(self, db: Any):
        self.db = db

    def list(self, collection: str, user_id: str) -> list[dict]:
        if hasattr(self.db, "_all"):
            try:
                return [dict(x) for x in self.db._all(collection, user_id)]
            except (KeyError, TypeError):
                pass
        bucket = getattr(self.db, "_data", {}).setdefault(collection, {})
        return [dict(x) for x in bucket.values() if x.get("user_id") == user_id]

    def get(self, collection: str, key: str, user_id: str) -> Optional[dict]:
        if hasattr(self.db, "_get"):
            try:
                value = self.db._get(collection, key, user_id)
                return dict(value) if value else None
            except (KeyError, TypeError):
                pass
        value = getattr(self.db, "_data", {}).setdefault(collection, {}).get(key)
        return dict(value) if value and value.get("user_id") == user_id else None

    def save(self, collection: str, value: dict) -> dict:
        if hasattr(self.db, "_save"):
            try:
                return dict(self.db._save(collection, value))
            except (KeyError, TypeError):
                pass
        getattr(self.db, "_data", {}).setdefault(collection, {})[str(value["id"])] = dict(value)
        return dict(value)

    def delete(self, collection: str, key: str, user_id: str) -> bool:
        if hasattr(self.db, "_delete"):
            try:
                return bool(self.db._delete(collection, key, user_id))
            except (KeyError, TypeError):
                pass
        bucket = getattr(self.db, "_data", {}).setdefault(collection, {})
        value = bucket.get(key)
        if value and value.get("user_id") == user_id:
            del bucket[key]
            return True
        return False


def install_mobile_oauth(auth_module: Any) -> None:
    """Allow Google ID tokens minted for either the website or Android app."""
    from google.oauth2 import id_token

    android_client_id = os.environ.get(
        "GOOGLE_ANDROID_CLIENT_ID",
        "611514829994-fuh73027tm7ru5npv3a84mfq3afkm345.apps.googleusercontent.com",
    )
    allowed = {auth_module.GOOGLE_CLIENT_ID, android_client_id}

    def verify(token: str) -> dict:
        claims = id_token.verify_oauth2_token(token, auth_module._request)
        if claims.get("aud") not in allowed:
            raise ValueError("Token audience is not an approved WEFT OAuth client")
        return claims

    auth_module.verify_google_id_token = verify


def install_weft_sync(app: Any, db: Any, get_current_user: Callable, privacy: Any) -> None:
    store = Store(db)
    router = APIRouter(prefix="/api")

    def public(value: Optional[dict]) -> Optional[dict]:
        if not value:
            return None
        return {k: v for k, v in value.items() if k not in {"user_id", "push_token"}}

    def log(user_id: str, kind: str, message: str, task_id=None, device_id=None) -> None:
        event_id = int(datetime.now(timezone.utc).timestamp() * 1_000_000)
        store.save("activities", {"id": event_id, "user_id": user_id, "kind": kind, "message": message[:1000], "task_id": task_id, "device_id": device_id, "created_at": _now()})

    @router.get("/work-state")
    def get_work_state(user: dict = Depends(get_current_user)):
        return public(store.get("work_states", user["id"], user["id"]))

    @router.put("/work-state")
    def put_work_state(body: WorkStateIn, user: dict = Depends(get_current_user)):
        prior = store.get("work_states", user["id"], user["id"])
        value = {"id": user["id"], "user_id": user["id"], **body.model_dump(), "updated_at": _now()}
        if body.browser_context:
            value["browser_context"]["url"] = _safe_url(body.browser_context.url, privacy)
            value["browser_context"]["captured_at"] = body.browser_context.captured_at or _now()
        saved = store.save("work_states", value)
        changed = not prior or prior.get("status") != body.status or prior.get("last_activity") != body.last_activity
        if changed:
            log(user["id"], f"WORK_{body.status}", body.last_activity or body.current_step or "Work state updated", body.task_id, body.device_id)
        return public(saved)

    @router.get("/activity")
    def list_activity(user: dict = Depends(get_current_user)):
        rows = sorted(store.list("activities", user["id"]), key=lambda x: x.get("created_at", ""), reverse=True)
        return [public(x) for x in rows[:200]]

    @router.get("/devices")
    def list_devices(user: dict = Depends(get_current_user)):
        rows = sorted(store.list("devices", user["id"]), key=lambda x: x.get("last_seen", ""), reverse=True)
        return [public(x) for x in rows]

    @router.post("/devices")
    def register_device(body: DeviceIn, user: dict = Depends(get_current_user)):
        value = {"id": f'{user["id"]}:{body.device_id}', "user_id": user["id"], **body.model_dump(), "last_seen": _now()}
        return public(store.save("devices", value))

    @router.post("/devices/{device_id}/heartbeat")
    def heartbeat(device_id: str, user: dict = Depends(get_current_user)):
        key = f'{user["id"]}:{device_id}'
        current = store.get("devices", key, user["id"])
        if not current:
            raise HTTPException(status_code=404, detail="Device is not registered")
        current["last_seen"] = _now()
        return public(store.save("devices", current))

    @router.delete("/devices/{device_id}")
    def delete_device(device_id: str, user: dict = Depends(get_current_user)):
        return {"deleted": store.delete("devices", f'{user["id"]}:{device_id}', user["id"])}

    @router.post("/devices/push/test")
    def test_push(user: dict = Depends(get_current_user)):
        """Send a visible, user-requested FCM test to this account's phones."""
        try:
            from firebase_admin import messaging
        except ImportError as exc:
            raise HTTPException(status_code=503, detail="Firebase messaging is unavailable") from exc
        sent = failed = 0
        for device in store.list("devices", user["id"]):
            token = device.get("push_token")
            if not token:
                continue
            try:
                messaging.send(messaging.Message(
                    token=token,
                    notification=messaging.Notification(title="WEFT is connected", body="Notifications are ready on this device."),
                    data={"kind": "CONNECTION_TEST"},
                    android=messaging.AndroidConfig(priority="high", notification=messaging.AndroidNotification(channel_id="work")),
                ))
                sent += 1
            except Exception:  # stale/invalid tokens are counted without leaking them
                failed += 1
        return {"sent": sent, "failed": failed}

    @router.put("/browser-context")
    def set_browser_context(body: BrowserContextIn, user: dict = Depends(get_current_user)):
        state = store.get("work_states", user["id"], user["id"])
        if not state or state.get("status") not in {"ACTIVE", "PAUSED"}:
            raise HTTPException(status_code=409, detail="Browser context requires an active work thread")
        context = body.model_dump()
        context["url"] = _safe_url(body.url, privacy)
        context["captured_at"] = body.captured_at or _now()
        state["browser_context"] = context
        state["updated_at"] = _now()
        store.save("work_states", state)
        return context

    app.include_router(router)
