import sys, pathlib
sys.path.insert(0, str(pathlib.Path(__file__).parent))
from avatars import AVATARS
from render import render_many
out = pathlib.Path(sys.argv[1] if len(sys.argv) > 1 else "/tmp/artout/avatars")
render_many([(f(), out / f"avatar_{i}.png", 512, 512) for i, f in enumerate(AVATARS)])
print("done", out)
