"""Structured adapter exceptions with stable error codes."""
from __future__ import annotations


class AdapterError(Exception):
    def __init__(self, code: str, message: str, stage: str = "adapter", details: dict | None = None):
        super().__init__(message)
        self.code = code
        self.message = message
        self.stage = stage
        self.details = details or {}

    def to_dict(self) -> dict:
        return {
            "code": self.code,
            "message": self.message,
            "stage": self.stage,
            "details": self.details,
        }
