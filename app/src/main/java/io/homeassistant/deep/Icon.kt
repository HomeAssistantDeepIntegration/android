package io.homeassistant.deep

private val BATTERY_ICONS = mapOf(
    10 to "cmd-battery-10",
    20 to "cmd-battery-20",
    30 to "cmd-battery-30",
    40 to "cmd-battery-40",
    50 to "cmd-battery-50",
    60 to "cmd-battery-60",
    70 to "cmd-battery-70",
    80 to "cmd-battery-80",
    90 to "cmd-battery-90",
    100 to "cmd-battery"
)

private val BATTERY_CHARGING_ICONS = mapOf(
    10 to "cmd-battery-charging-10",
    20 to "cmd-battery-charging-20",
    30 to "cmd-battery-charging-30",
    40 to "cmd-battery-charging-40",
    50 to "cmd-battery-charging-50",
    60 to "cmd-battery-charging-60",
    70 to "cmd-battery-charging-70",
    80 to "cmd-battery-charging-80",
    90 to "cmd-battery-charging-90",
    100 to "cmd-battery-charging"
)

fun getStateIcon(
    state: EntityState
): String {
    val icon = state.attributes["icon"] as String?
    if (icon != null) return icon.replace(
        "mdi:", "cmd-"
    ).replace(':', '-')

    val deviceClass = state.attributes["device_class"] as String?
    return when (state.entityId.split(".")[0]) {
        "alarm_control_panel" -> {
            // https://github.com/home-assistant/core/blob/dev/homeassistant/components/alarm_control_panel/icons.json
            when (deviceClass) {
                "armed_away" -> "cmd-shield-lock"
                "armed_custom_bypass" -> "cmd-security"
                "armed_home" -> "cmd-shield-home"
                "armed_night" -> "cmd-shield-moon"
                "armed_vacation" -> "cmd-shield-airplane"
                "disarmed" -> "cmd-shield-off"
                "pending" -> "cmd-shield-outline"
                "triggered" -> "cmd-bell-ring"
                else -> "cmd-shield"
            }
        }

        "automation" -> {
            // https://github.com/home-assistant/core/blob/dev/homeassistant/components/automation/icons.json
            when (state.state) {
                "off" -> "cmd-robot-off"
                "unavailable" -> "cmd-robot-confused"
                else -> "cmd-robot"
            }
        }

        "binary_sensor" -> {
            // https://github.com/home-assistant/core/blob/dev/homeassistant/components/binary_sensor/icons.json
            when (deviceClass) {
                "battery" -> {
                    when (state.state) {
                        "on" -> "cmd-battery-outline"
                        else -> "cmd-battery"
                    }
                }

                "battery_charging" -> {
                    when (state.state) {
                        "on" -> "cmd-battery-charging"
                        else -> "cmd-battery"
                    }
                }

                "carbon_monoxide" -> {
                    when (state.state) {
                        "on" -> "cmd-smoke-detector-alert"
                        else -> "cmd-smoke-detector"
                    }
                }

                "cold" -> {
                    when (state.state) {
                        "on" -> "cmd-snowflake"
                        else -> "cmd-thermometer"
                    }
                }

                "connectivity" -> {
                    when (state.state) {
                        "on" -> "cmd-check-network-outline"
                        else -> "cmd-close-network-outline"
                    }
                }

                "door" -> {
                    when (state.state) {
                        "on" -> "cmd-door-open"
                        else -> "cmd-door-closed"
                    }
                }

                "garage_door" -> {
                    when (state.state) {
                        "on" -> "cmd-garage-open"
                        else -> "cmd-garage"
                    }
                }

                "gas" -> {
                    when (state.state) {
                        "on" -> "cmd-alert-circle"
                        else -> "cmd-check-circle"
                    }
                }

                "heat" -> {
                    when (state.state) {
                        "on" -> "cmd-fire"
                        else -> "cmd-thermometer"
                    }
                }

                "light" -> {
                    when (state.state) {
                        "on" -> "cmd-brightness-7"
                        else -> "cmd-brightness-5"
                    }
                }

                "lock" -> {
                    when (state.state) {
                        "on" -> "cmd-lock-open"
                        else -> "cmd-lock"
                    }
                }

                "moisture" -> {
                    when (state.state) {
                        "on" -> "cmd-water"
                        else -> "cmd-water-off"
                    }
                }

                "motion" -> {
                    when (state.state) {
                        "on" -> "cmd-motion-sensor"
                        else -> "cmd-motion-sensor-off"
                    }
                }

                "moving" -> {
                    when (state.state) {
                        "on" -> "cmd-octagon"
                        else -> "cmd-arrow-right"
                    }
                }

                "occupancy" -> {
                    when (state.state) {
                        "on" -> "cmd-home"
                        else -> "cmd-home-outline"
                    }
                }

                "opening" -> {
                    when (state.state) {
                        "on" -> "cmd-square-outline"
                        else -> "cmd-square"
                    }
                }

                "plug" -> {
                    when (state.state) {
                        "on" -> "cmd-power-plug"
                        else -> "cmd-power-plug-off"
                    }
                }

                "power" -> {
                    when (state.state) {
                        "on" -> "cmd-power-plug"
                        else -> "cmd-power-plug-off"
                    }
                }

                "presence" -> {
                    when (state.state) {
                        "on" -> "cmd-home"
                        else -> "cmd-home-outline"
                    }
                }

                "problem" -> {
                    when (state.state) {
                        "on" -> "cmd-alert-circle"
                        else -> "cmd-check-circle"
                    }
                }

                "running" -> {
                    when (state.state) {
                        "on" -> "cmd-play"
                        else -> "cmd-stop"
                    }
                }

                "safety" -> {
                    when (state.state) {
                        "on" -> "cmd-alert-circle"
                        else -> "cmd-check-circle"
                    }
                }

                "smoke" -> {
                    when (state.state) {
                        "on" -> "cmd-smoke-detector-variant-alert"
                        else -> "cmd-smoke-detector-variant"
                    }
                }

                "sound" -> {
                    when (state.state) {
                        "on" -> "cmd-music-note"
                        else -> "cmd-music-note-off"
                    }
                }

                "tamper" -> {
                    when (state.state) {
                        "on" -> "cmd-alert-circle"
                        else -> "cmd-check-circle"
                    }
                }

                "update" -> {
                    when (state.state) {
                        "on" -> "cmd-package-up"
                        else -> "cmd-package"
                    }
                }

                "vibration" -> {
                    when (state.state) {
                        "on" -> "cmd-vibrate"
                        else -> "cmd-crop-portrait"
                    }
                }

                "window" -> {
                    when (state.state) {
                        "on" -> "cmd-window-open"
                        else -> "cmd-window-closed"
                    }
                }

                else -> {
                    when (state.state) {
                        "on" -> "cmd-checkbox-marked-circle"
                        else -> "cmd-radiobox-blank"
                    }
                }
            }
        }

        "button" -> {
            // https://github.com/home-assistant/core/blob/dev/homeassistant/components/button/icons.json
            when (deviceClass) {
                "restart" -> "cmd-restart"
                "identify" -> "cmd-crosshairs-question"
                "update" -> "cmd-package-up"
                else -> "cmd-button-pointer"
            }
        }

        "calendar" -> {
            // https://github.com/home-assistant/core/blob/dev/homeassistant/components/calendar/icons.json
            when (state.state) {
                "on" -> "mdi:calendar-check"
                "off" -> "cmd-calendar-blank"
                else -> "cmd-calendar"
            }
        }

        "camera" -> {
            // https://github.com/home-assistant/core/blob/dev/homeassistant/components/camera/icons.json
            when (state.state) {
                "off" -> "cmd-video-off"
                else -> "cmd-video"
            }
        }

        "climate" -> {
            // https://github.com/home-assistant/core/blob/dev/homeassistant/components/climate/icons.json
            "cmd-thermostat"
        }

        "conversation" -> {
            // https://github.com/home-assistant/core/blob/dev/homeassistant/components/conversation/icons.json
            "cmd-forum-outline"
        }

        "cover" -> {
            // https://github.com/home-assistant/core/blob/dev/homeassistant/components/cover/icons.json
            when (deviceClass) {
                "blind" -> {
                    when (state.state) {
                        "closed" -> "cmd-blinds-horizontal-closed"
                        "closing" -> "cmd-arrow-down-box"
                        "opening" -> "cmd-arrow-up-box"
                        else -> "cmd-blinds-horizontal"
                    }
                }

                "curtain" -> {
                    when (state.state) {
                        "closed" -> "cmd-curtains-closed"
                        "closing" -> "cmd-arrow-collapse-horizontal"
                        "opening" -> "cmd-arrow-split-vertical"
                        else -> "cmd-curtains"
                    }
                }

                "damper" -> {
                    when (state.state) {
                        "closed" -> "cmd-circle-slice-8"
                        else -> "cmd-circle"
                    }
                }

                "door" -> {
                    when (state.state) {
                        "closed" -> "cmd-door-closed"
                        else -> "cmd-door-open"
                    }
                }

                "garage" -> {
                    when (state.state) {
                        "closed" -> "cmd-garage"
                        "closing" -> "cmd-arrow-down-box"
                        "opening" -> "cmd-arrow-up-box"
                        else -> "cmd-garage-open"
                    }
                }

                "gate" -> {
                    when (state.state) {
                        "closed" -> "cmd-gate"
                        "closing" -> "cmd-arrow-right"
                        "opening" -> "cmd-arrow-right"
                        else -> "cmd-gate-open"
                    }
                }

                "shade" -> {
                    when (state.state) {
                        "closed" -> "cmd-roller-shade-closed"
                        "closing" -> "cmd-arrow-down-box"
                        "opening" -> "cmd-arrow-up-box"
                        else -> "cmd-roller-shade"
                    }
                }

                "shutter" -> {
                    when (state.state) {
                        "closed" -> "cmd-window-shutter"
                        "closing" -> "cmd-arrow-down-box"
                        "opening" -> "cmd-arrow-up-box"
                        else -> "cmd-window-shutter-open"
                    }
                }

                "window" -> {
                    when (state.state) {
                        "closed" -> "cmd-window-closed"
                        "closing" -> "cmd-arrow-down-box"
                        "opening" -> "cmd-arrow-up-box"
                        else -> "cmd-window-open"
                    }
                }

                else -> {
                    when (state.state) {
                        "closed" -> "cmd-window-closed"
                        "closing" -> "cmd-arrow-down-box"
                        "opening" -> "cmd-arrow-up-box"
                        else -> "cmd-window-open"
                    }
                }
            }
        }

        "device_tracker" -> {
            // https://github.com/home-assistant/core/blob/dev/homeassistant/components/device_tracker/icons.json
            when (state.attributes["source_type"]) {
                "router" -> {
                    when (state.state) {
                        "home" -> "cmd-lan-connect"
                        else -> "cmd-lan-disconnect"
                    }
                }

                "bluetooth", "bluetooth_le" -> {
                    when (state.state) {
                        "home" -> "cmd-bluetooth-connect"
                        else -> "cmd-bluetooth"
                    }
                }

                else -> {
                    when (state.state) {
                        "not_home" -> "cmd-account-arrow-right"
                        else -> "cmd-account"
                    }
                }
            }
        }

        "event" -> {
            // https://github.com/home-assistant/core/blob/dev/homeassistant/components/event/icons.json
            when (deviceClass) {
                "button" -> "cmd-gesture-tap-button"
                "doorbell" -> "cmd-doorbell"
                "motion" -> "cmd-motion-sensor"
                else -> "cmd-eye-check"
            }
        }

        "input_boolean" -> {
            // https://github.com/home-assistant/core/blob/dev/homeassistant/components/input_boolean/icons.json
            when (state.state) {
                "off" -> "cmd-close-circle-outline"
                else -> "cmd-check-circle-outline"
            }
        }

        "light" -> {
            // https://github.com/home-assistant/core/blob/dev/homeassistant/components/light/icons.json
            "cmd-lightbulb"
        }

        "media_player" -> {
            // https://github.com/home-assistant/core/blob/dev/homeassistant/components/media_player/icons.json

            when (deviceClass) {
                "receiver" -> {
                    when (state.state) {
                        "off" -> "cmd-audio-video-off"
                        else -> "cmd-audio-video"
                    }
                }

                "speaker" -> {
                    when (state.state) {
                        "off" -> "cmd-speaker-off"
                        "paused" -> "cmd-speaker-pause"
                        "playing" -> "cmd-speaker-play"
                        else -> "cmd-speaker"
                    }
                }

                "tv" -> {
                    when (state.state) {
                        "off" -> "cmd-television-off"
                        "paused" -> "cmd-television-pause"
                        "playing" -> "cmd-television-play"
                        else -> "cmd-television"
                    }
                }

                else -> {
                    when (state.state) {
                        "off" -> "cmd-cast-off"
                        "paused" -> "cmd-cast-connected"
                        "playing" -> "cmd-cast-connected"
                        else -> "cmd-cast"
                    }
                }
            }
        }

        "number" -> {
            // https://github.com/home-assistant/core/blob/dev/homeassistant/components/number/icons.json

            when (deviceClass) {
                "apparent_power" -> "cmd-flash"
                "aqi" -> "cmd-air-filter"
                "atmospheric_pressure" -> "cmd-thermometer-lines"
                "battery" -> "cmd-battery"
                "carbon_dioxide" -> "cmd-molecule-co2"
                "carbon_monoxide" -> "cmd-molecule-co"
                "current" -> "cmd-current-ac"
                "data_rate" -> "cmd-transmission-tower"
                "data_size" -> "cmd-database"
                "distance" -> "cmd-arrow-left-right"
                "duration" -> "cmd-progress-clock"
                "energy" -> "cmd-lightning-bolt"
                "energy_storage" -> "cmd-car-battery"
                "frequency" -> "cmd-sine-wave"
                "gas" -> "cmd-meter-gas"
                "humidity" -> "cmd-water-percent"
                "illuminance" -> "cmd-brightness-5"
                "irradiance" -> "cmd-sun-wireless"
                "moisture" -> "cmd-water-percent"
                "monetary" -> "cmd-cash"
                "nitrogen_dioxide" -> "cmd-molecule"
                "nitrogen_monoxide" -> "cmd-molecule"
                "nitrous_oxide" -> "cmd-molecule"
                "ozone" -> "cmd-molecule"
                "ph" -> "cmd-ph"
                "pm1" -> "cmd-molecule"
                "pm10" -> "cmd-molecule"
                "pm25" -> "cmd-molecule"
                "power" -> "cmd-flash"
                "power_factor" -> "cmd-angle-acute"
                "precipitation" -> "cmd-weather-rainy"
                "precipitation_intensity" -> "cmd-weather-pouring"
                "pressure" -> "cmd-gauge"
                "reactive_power" -> "cmd-flash"
                "signal_strength" -> "cmd-wifi"
                "sound_pressure" -> "cmd-ear-hearing"
                "speed" -> "cmd-speedometer"
                "sulfur_dioxide" -> "cmd-molecule"
                "temperature" -> "cmd-thermometer"
                "volatile_organic_compounds" -> "cmd-molecule"
                "volatile_organic_compounds_parts" -> "cmd-molecule"
                "voltage" -> "cmd-sine-wave"
                "volume" -> "cmd-car-coolant-level"
                "volume_storage" -> "cmd-storage-tank"
                "water" -> "cmd-water"
                "weight" -> "cmd-weight"
                "wind_speed" -> "cmd-weather-windy"
                else -> "cmd-ray-vertex"
            }
        }

        "person" -> {
            // https://github.com/home-assistant/core/blob/dev/homeassistant/components/person/icons.json

            when (state.state) {
                "not_home" -> "cmd-account-arrow-right"
                else -> "cmd-account"
            }
        }

        "remote" -> {
            // https://github.com/home-assistant/core/blob/dev/homeassistant/components/remote/icons.json

            when (state.state) {
                "off" -> "cmd-remote-off"
                else -> "cmd-remote"
            }
        }

        "scene" -> {
            // https://github.com/home-assistant/core/blob/dev/homeassistant/components/scene/icons.json

            "cmd-palette"
        }

        "script" -> {
            // https://github.com/home-assistant/core/blob/dev/homeassistant/components/script/icons.json

            when (state.state) {
                "on" -> "cmd-script-text-play"
                else -> "cmd-script-text"
            }
        }

        "select" -> {
            // https://github.com/home-assistant/core/blob/dev/homeassistant/components/select/icons.json

            "cmd-format-list-bulleted"
        }

        "sensor" -> {
            // https://github.com/home-assistant/core/blob/dev/homeassistant/components/sensor/icons.json

            when (deviceClass) {
                "battery" -> {
                    val batteryValue = state.state.toIntOrNull() ?: return when (state.state) {
                        "off" -> "cmd-battery"
                        "on" -> "cmd-battery-alert"
                        else -> "cmd-battery-unknown"
                    }

                    val batteryRound = (batteryValue / 10) * 10
                    return when {
                        batteryValue <= 5 -> "cmd-battery-alert-variant-outline"
                        else -> BATTERY_ICONS[batteryRound] ?: "cmd-battery-unknown"
                    }
                }

                "apparent_power" -> "cmd-flash"
                "aqi" -> "cmd-air-filter"
                "atmospheric_pressure" -> "cmd-thermometer-lines"
                "carbon_dioxide" -> "cmd-molecule-co2"
                "carbon_monoxide" -> "cmd-molecule-co"
                "current" -> "cmd-current-ac"
                "data_rate" -> "cmd-transmission-tower"
                "data_size" -> "cmd-database"
                "date" -> "cmd-calendar"
                "distance" -> "cmd-arrow-left-right"
                "duration" -> "cmd-progress-clock"
                "energy" -> "cmd-lightning-bolt"
                "energy_storage" -> "cmd-car-battery"
                "enum" -> "cmd-eye"
                "frequency" -> "cmd-sine-wave"
                "gas" -> "cmd-meter-gas"
                "humidity" -> "cmd-water-percent"
                "illuminance" -> "cmd-brightness-5"
                "irradiance" -> "cmd-sun-wireless"
                "moisture" -> "cmd-water-percent"
                "monetary" -> "cmd-cash"
                "nitrogen_dioxide" -> "cmd-molecule"
                "nitrogen_monoxide" -> "cmd-molecule"
                "nitrous_oxide" -> "cmd-molecule"
                "ozone" -> "cmd-molecule"
                "ph" -> "cmd-ph"
                "pm1" -> "cmd-molecule"
                "pm10" -> "cmd-molecule"
                "pm25" -> "cmd-molecule"
                "power" -> "cmd-flash"
                "power_factor" -> "cmd-angle-acute"
                "precipitation" -> "cmd-weather-rainy"
                "precipitation_intensity" -> "cmd-weather-pouring"
                "pressure" -> "cmd-gauge"
                "reactive_power" -> "cmd-flash"
                "signal_strength" -> "cmd-wifi"
                "sound_pressure" -> "cmd-ear-hearing"
                "speed" -> "cmd-speedometer"
                "sulphur_dioxide" -> "cmd-molecule"
                "temperature" -> "cmd-thermometer"
                "timestamp" -> "cmd-clock"
                "volatile_organic_compounds" -> "cmd-molecule"
                "volatile_organic_compounds_parts" -> "cmd-molecule"
                "voltage" -> "cmd-sine-wave"
                "volume" -> "cmd-car-coolant-level"
                "volume_storage" -> "cmd-storage-tank"
                "water" -> "cmd-water"
                "weight" -> "cmd-weight"
                "wind_speed" -> "cmd-weather-windy"
                else -> "cmd-eye"
            }
        }

        "stt" -> {
            // https://github.com/home-assistant/core/blob/dev/homeassistant/components/stt/icons.json

            "cmd-microphone-message"
        }

        "sun" -> {
            // https://github.com/home-assistant/core/blob/dev/homeassistant/components/sun/icons.json

            "cmd-weather-sunny"
        }

        "switch" -> {
            // https://github.com/home-assistant/core/blob/dev/homeassistant/components/switch/icons.json

            when (deviceClass) {
                "switch" -> {
                    when (state.state) {
                        "off" -> "cmd-toggle-switch-variant-off"
                        else -> "cmd-toggle-switch-variant"
                    }
                }

                "outlet" -> {
                    when (state.state) {
                        "off" -> "cmd-power-plug-off"
                        else -> "cmd-power-plug"
                    }
                }

                else -> {
                    when (state.state) {
                        "off" -> "cmd-toggle-switch-variant-off"
                        else -> "cmd-toggle-switch-variant"
                    }
                }
            }
        }

        "tag" -> {
            // https://github.com/home-assistant/core/blob/dev/homeassistant/components/tag/icons.json

            "cmd-tag-outline"
        }

        "text" -> {
            // https://github.com/home-assistant/core/blob/dev/homeassistant/components/text/icons.json

            "cmd-form-textbox"
        }

        "todo" -> {
            "cmd-clipboard-list"
        }

        "tts" -> {
            // https://github.com/home-assistant/core/blob/dev/homeassistant/components/tts/icons.json

            "cmd-speaker-message"
        }

        "update" -> {
            // https://github.com/home-assistant/core/blob/dev/homeassistant/components/update/icons.json

            when (state.state) {
                "off" -> "cmd-package"
                else -> "cmd-package-up"
            }
        }

        "wake_word" -> {
            // https://github.com/home-assistant/core/blob/dev/homeassistant/components/wake_word/icons.json

            "cmd-chat-sleep"
        }

        "weather" -> {
            // https://github.com/home-assistant/core/blob/dev/homeassistant/components/weather/icons.json

            when (state.state) {
                "clear-night" -> "cmd-weather-night"
                "cloudy" -> "cmd-weather-cloudy"
                "exceptional" -> "cmd-alert-circle-outline"
                "fog" -> "cmd-weather-fog"
                "hail" -> "cmd-weather-hail"
                "lightning" -> "cmd-weather-lightning"
                "lightning-rainy" -> "cmd-weather-lightning-rainy"
                "pouring" -> "cmd-weather-pouring"
                "rainy" -> "cmd-weather-rainy"
                "snowy" -> "cmd-weather-snowy"
                "snowy-rainy" -> "cmd-weather-snowy-rainy"
                "sunny" -> "cmd-weather-sunny"
                "windy" -> "cmd-weather-windy"
                "windy-variant" -> "cmd-weather-windy-variant"
                else -> "cmd-weather-partly-cloudy"
            }
        }

        else -> "cmd-eye"
    }
}
