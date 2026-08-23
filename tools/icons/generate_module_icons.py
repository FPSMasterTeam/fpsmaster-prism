#!/usr/bin/env python3
"""Fetch the shared ClickGUI module icon set from Lucide via Iconify."""

from pathlib import Path
from urllib.request import Request, urlopen


ICONS = {
    "animation": "gallery-horizontal-end",
    "armor-display": "shield",
    "auto-gg": "message-circle-check",
    "auto-text": "message-square-text",
    "better-chat": "messages-square",
    "better-fishing-rod": "fish",
    "better-screen": "panels-top-left",
    "block-indicator": "focus",
    "block-overlay": "square-dashed",
    "chat-avatars": "user-round",
    "clean-view": "eye",
    "clickgui": "layout-dashboard",
    "client-settings": "settings-2",
    "clock-display": "clock-3",
    "combo-display": "tally-4",
    "coords-display": "map-pin",
    "cps-display": "mouse-pointer-click",
    "crosshair": "crosshair",
    "custom-fog": "cloud-fog",
    "custom-fov": "scan",
    "custom-titles": "heading",
    "damage-indicator": "heart-pulse",
    "direction-display": "compass",
    "dragon-wings": "feather",
    "fire-modifier": "flame",
    "fixed-inventory": "backpack",
    "fps-display": "gauge",
    "free-look": "rotate-3d",
    "full-bright": "sun",
    "hide-indicator": "eye-off",
    "hit-boxes": "boxes",
    "hit-color": "paintbrush",
    "hitboxes": "boxes",
    "inventory-display": "package-open",
    "item-count-display": "list-plus",
    "item-physics": "orbit",
    "keystrokes": "keyboard",
    "level-tag": "badge",
    "lyrics-display": "list-music",
    "mini-map": "radar",
    "minimized-bobbing": "move-vertical",
    "mods-list": "list",
    "more-particles": "sparkles",
    "motion-blur": "fast-forward",
    "name-protect": "shield-user",
    "nametags": "badge",
    "no-hit-delay": "timer-off",
    "no-hurt-cam": "video-off",
    "old-animations": "history",
    "optimization": "gauge",
    "particles-modifier": "atom",
    "performance": "gauge",
    "performance-hud": "chart-no-axes-column-increasing",
    "ping-display": "radio",
    "play-time": "timer",
    "player-display": "user-round",
    "potion-display": "flask-conical",
    "raw-input": "mouse",
    "reach-display": "ruler",
    "replay": "rotate-ccw",
    "saturation-display": "heart",
    "scoreboard": "list-ordered",
    "server-address-display": "server",
    "smooth-zoom": "zoom-in",
    "sound-modifier": "audio-lines",
    "sprint": "rabbit",
    "tab-overlay": "rows-3",
    "target-display": "scan-face",
    "time-changer": "clock-arrow-up",
    "tnt-timer": "bomb",
    "toggle-sneak": "footprints",
    "wavy-cape": "flag-triangle-right",
}


def main() -> None:
    target = Path(__file__).resolve().parents[2] / "docs" / "icons" / "modules"
    target.mkdir(parents=True, exist_ok=True)
    for module_id, icon in ICONS.items():
        url = f"https://api.iconify.design/lucide/{icon}.svg?color=%23ffffff"
        svg = urlopen(Request(url, headers={"User-Agent": "FPSMaster icon generator"})).read().decode("utf-8")
        svg = svg.replace('width="1em" height="1em"', 'width="24" height="24"')
        (target / f"{module_id}.svg").write_text(svg + "\n", encoding="utf-8")
        print(f"{module_id}: {icon}")


if __name__ == "__main__":
    main()
