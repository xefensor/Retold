import json
import sys
from pathlib import Path


def remove_recipe_rewards(file_path: Path) -> bool:
    with file_path.open("r", encoding="utf-8") as file:
        data = json.load(file)

    rewards = data.get("rewards")

    if not isinstance(rewards, dict):
        return False

    if "recipes" not in rewards:
        return False

    del rewards["recipes"]

    # Pokud rewards zůstane prázdné, smažeme celý rewards objekt.
    if not rewards:
        del data["rewards"]

    with file_path.open("w", encoding="utf-8") as file:
        json.dump(data, file, indent=2, ensure_ascii=False)
        file.write("\n")

    return True


def main():
    if len(sys.argv) != 2:
        print("Usage: python remove_recipe_rewards.py <advancement_folder>")
        print("Example: python remove_recipe_rewards.py src/main/resources/data/minecraft/advancement")
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
            if remove_recipe_rewards(file_path):
                print(f"Removed recipe rewards: {file_path}")
                changed += 1
            else:
                skipped += 1
        except Exception as exception:
            print(f"Failed: {file_path}")
            print(exception)
            failed += 1

    print()
    print("Done.")
    print(f"Changed: {changed}")
    print(f"Skipped: {skipped}")
    print(f"Failed: {failed}")


if __name__ == "__main__":
    main()
