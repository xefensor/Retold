import json
import sys
from pathlib import Path


def add_hidden_to_display(file_path: Path) -> bool:
    with file_path.open("r", encoding="utf-8") as file:
        data = json.load(file)

    display = data.get("display")

    if not isinstance(display, dict):
        return False

    # Pokud hidden už existuje, smažeme ho a přidáme znovu,
    # aby bylo poslední položkou v display.
    if "hidden" in display:
        del display["hidden"]

    display["hidden"] = True

    with file_path.open("w", encoding="utf-8") as file:
        json.dump(data, file, indent=2, ensure_ascii=False)
        file.write("\n")

    return True


def main():
    if len(sys.argv) != 2:
        print("Usage: python hide_advancements.py <advancement_folder>")
        print("Example: python hide_advancements.py src/main/resources/data/minecraft/advancement")
        return

    root = Path(sys.argv[1])

    if not root.exists():
        print(f"Folder does not exist: {root}")
        return

    changed = 0
    skipped = 0
    failed = 0

    for file_path in root.rglob("*.json"):
        try:
            if add_hidden_to_display(file_path):
                print(f"Updated: {file_path}")
                changed += 1
            else:
                print(f"Skipped, no display: {file_path}")
                skipped += 1
        except Exception as exception:
            print(f"Failed: {file_path}")
            print(exception)
            failed += 1

    print()
    print(f"Done.")
    print(f"Updated: {changed}")
    print(f"Skipped: {skipped}")
    print(f"Failed: {failed}")


if __name__ == "__main__":
    main()
