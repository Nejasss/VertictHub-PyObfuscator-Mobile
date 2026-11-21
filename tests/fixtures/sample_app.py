"""Sample fixture script used for adapter unit and integration testing."""
from __future__ import annotations

SECRET_API_BANNER = "VertictInternalToken_9981"

class DataProcessor:
    def __init__(self, prefix: str) -> None:
        self.prefix = prefix

    def transform_item(self, value: int) -> str:
        computed = (value * 7) + 13
        return f"{self.prefix}:{computed}:{SECRET_API_BANNER}"

def main() -> str:
    proc = DataProcessor("UNIT")
    return proc.transform_item(6)

if __name__ == "__main__":
    print(main())
