from pathlib import Path
import json
import shutil
import zipfile

root = Path(__file__).resolve().parents[1]
output = root / 'anatomy-studio-complete-kit.zip'
files = []
for path in root.rglob('*'):
    relative = path.relative_to(root)
    if not path.is_file() or any(p in {'node_modules', '.git'} for p in relative.parts):
        continue
    if path.suffix.lower() == '.zip':
        continue
    files.append(path)
with zipfile.ZipFile(output, 'w', zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
    for path in sorted(files):
        archive.write(path, path.relative_to(root).as_posix())
with zipfile.ZipFile(output) as archive:
    assert archive.testzip() is None
    names = archive.namelist()
    assert len([n for n in names if n.startswith('play-store/') and n.endswith('.png')]) == 10
    for name in ['anatomy-studio-launch-1080p.mp4', 'anatomy-studio-vertical-1080p.mp4', 'README.md', 'remotion/src/index.tsx', 'remotion/public/score-master.wav', 'remotion/package.json', 'remotion/package-lock.json']:
        assert name in names, name
destination = Path('C:/Users/FAJAR/OpenBot/Shared/AnatomyStudioLaunch')
destination.mkdir(parents=True, exist_ok=True)
deliverables = [output.name, 'anatomy-studio-launch-1080p.mp4', 'anatomy-studio-vertical-1080p.mp4', 'screenshot-collection-preview.jpg']
for name in deliverables:
    shutil.copy2(root / name, destination / name)
print(json.dumps({'zip_integrity': 'passed', 'entries': len(names), 'zip_bytes': output.stat().st_size, 'files': [str(destination / n) for n in deliverables], 'inventory': names}, indent=2))
