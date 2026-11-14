package com.snehonir.lipirx.common

object AppConstants {
    const val DATABASE_NAME = "lipirx.db"

    const val DATETIME_FORMAT_FULL_TIMESTAMP = "MMM dd, yyyy, hh:mm:ss a"
    const val DATE_FORMAT_SHORT = "MMM dd, yyyy"
    const val TIME_FORMAT_12H = "hh:mm:ss a"

    const val MODEL_PREFIX_GEMINI = "models/"

    const val MIME_TYPE_JPEG = "image/jpeg"
    const val MIME_TYPE_PNG = "image/png"
    const val MIME_TYPE_JSON = "application/json"

    const val EXTENSION_LITETRLM = ".litertlm"
    const val EXTENSION_TFLITE = ".tflite"
    const val EXTENSION_JPG = ".jpg"
    const val EXTENSION_JPEG = ".jpeg"
    const val EXTENSION_PNG = ".png"

    const val UNIT_KB = "KB"
    const val UNIT_MB = "MB"
    const val UNIT_GB = "GB"
    const val BYTES_PER_KB = 1024L
    const val BYTES_PER_MB = 1024L * 1024L

    const val GEMINI_DEFAULT_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models"
    const val GEMINI_DEFAULT_MODEL = "gemini-2.0-flash"

    const val GROQ_DEFAULT_ENDPOINT = "https://api.groq.com/openai/v1/models"
    const val GROQ_CHAT_COMPLETIONS_ENDPOINT = "https://api.groq.com/openai/v1/chat/completions"
    const val GROQ_DEFAULT_MODEL = "llama-3.2-90b-vision-instruct"

    const val OPENAI_DEFAULT_ENDPOINT = "https://api.openai.com/v1/models"
    const val OPENAI_CHAT_COMPLETIONS_ENDPOINT = "https://api.openai.com/v1/chat/completions"
    const val OPENAI_DEFAULT_MODEL = "gpt-4o"

    const val CUSTOM_DEFAULT_MODEL = "custom-vision-model"
    const val ON_DEVICE_DEFAULT_MODEL = "gemini-nano-local"

    const val DEFAULT_MCP_PORT = 8080
    const val DEFAULT_MCP_PATH = "/lipirx/mcp"
    const val DEFAULT_MCP_URL = "http://localhost:8080/lipirx/mcp"

    const val MAX_SUPERVISOR_TURNS = 6
    const val HTTP_TIMEOUT_MILLIS = 60_000L

    const val EVAL_ACCURACY_THRESHOLD = 0.8f
}
